# Redis architecture audit

Phase 1 audit; this document describes the existing implementation and proposals, not a production certification.

## Existing backend

- Maven, Java 17 target, Spring Boot 3.3.5; Spring Web, Security, JPA, Validation and Data Redis, MySQL/Flyway, and H2 for tests. Redis uses Spring Boot's Lettuce integration. No Actuator dependency or application cache manager is currently configured.
- Nine controllers expose authentication, role probes, bills, categories, menu items, notifications, public menu, store settings, and administrator reads. Services hold authentication, billing, catalog, public-menu and image operations; store, notifications and admin reads also access repositories directly from controllers.
- Nineteen repositories cover 17 schema tables and two reporting views. The authoritative SQL schema and Flyway migrations govern persistence; DTOs avoid returning user password or refresh-token hashes. Menu DTOs include estimated costs and should not be treated as public data.
- JWT cookies/bearer authentication loads current user details; refresh tokens are hashed and rotated in MySQL. Spring Security applies role rules and CSRF protection. Exception advice maps errors to HTTP responses; SLF4J logs errors and Redis fallback warnings.
- Properties files configure the default, dev and test profiles; `backend/.env` is an optional local properties import. There is no checked-in Docker Compose setup and `infra/README.md` is a placeholder. The test profile uses H2 and disables Flyway, so it cannot prove MySQL locking or migration behavior.

## Existing request flow

`GET /api/v1/public/menu` delegates to `PublicMenuService`, which invokes `PublicMenuCache` around a read-only transaction that loads store, active categories and available items. When enabled, it checks Redis, deserializes JSON into the explicit public-menu DTO, and loads MySQL on a miss. The single public DTO excludes authentication and item cost data. Redis is optional and errors fall back to MySQL; default connection and command timeouts are 500 ms, with a 10-second local retry cooldown. Concurrent loads are coalesced per application instance.

Keys are `<namespace>:public-menu:v1:revision` and `<namespace>:public-menu:v1:data:<revision>`; data entries expire after 30 seconds. On committed category, menu-item, image or store changes, an AFTER_COMMIT event increments the revision. A read started before that increment writes under its previously captured revision. Old data keys expire naturally. Failure of the post-commit Redis increment can leave an old entry visible to other instances until TTL expiry; direct database changes outside these event paths do not trigger invalidation. Browser-side caching can add approximately five minutes, as documented in the backend README. This is bounded eventual consistency, not a guarantee of immediate freshness after every successful mutation.

Authenticated category lists and unfiltered menu-item lists use keys under `<namespace>:public-menu:v1:data:<revision>:catalog:<variant>`, sharing the same revision and 30-second TTL. Item list DTOs include estimated costs, so Redis needs the same private-network and access-control protections as other staff data. Requests must still pass Spring Security. Filtered and individual item/category reads use MySQL; reads inside a write transaction bypass these catalog entries. Redis failure falls back to the database.

Phase 3 adds a **disabled-by-default, preparatory** Spring cache manager. Its one explicitly registered `public-menu-snapshot` region uses string keys, a versioned namespace, a concrete `PublicMenu` JSON serializer without default polymorphic typing, no null values, and a configurable positive TTL (30 seconds by default). Unregistered cache names cannot be created automatically, and writes/evictions are transaction-aware. It uses Spring Boot's existing Redis connection factory. No controller or service uses this region yet: it has a separate key prefix from the working revision-based cache and has no invalidation or Redis-error fallback implementation. Do not enable it for application traffic until later phases implement and test those behaviors. Its presence does not change the live public-menu flow above.

## Risks to resolve before production

- Validate the Redis failure and recovery policy across multiple instances, including revision loss/eviction and commit/invalidation races. Strict immediate freshness would require a different consistency design and cannot be promised by TTL and post-commit Redis operations alone.
- Production requires private network access, authentication and TLS where supported, environment isolation, deployment-specific secrets and monitoring. Redis URL credentials must never be logged. Current code exposes no cache hit/miss or latency metrics, and no Actuator health endpoint.
- Several list endpoints load unbounded data. Measure queries and response sizes before adding caches; cache storage and database load may otherwise grow with arbitrary search keys.
- The current RBAC implementation restricts admin reads to ADMIN, while `rules/03-rbac-and-security.md` says READ_ONLY views all routes. Resolve that policy discrepancy separately before asserting full role compliance; caching must not bypass either layer of authorization.
- Production safeguards depend on selecting the production profile. Validate MySQL checkout concurrency, migrations and recovery on a disposable MySQL instance before live sales.
