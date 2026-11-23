# Redis caching strategy

Redis is used for selected shared, read-heavy menu and catalog reads. Other controllers continue to use the database for the reasons below.

The existing cache-aside read and its database transaction are in `PublicMenuService`. Category lists and unfiltered menu-item lists also use the revision-based cache; a plain `@Cacheable` annotation would not reproduce its post-commit revision protocol or Redis failure fallback. The prepared Spring cache manager remains disabled. No mutation response is cached.

| Controller | Existing reads and writes | Redis decision | Reason and invalidation |
| --- | --- | --- | --- |
| PublicMenu | Public consolidated store, category and item read | Already cached, 30 seconds | Shared read-heavy snapshot. Category/menu create, update, delete, image and store updates publish an after-commit revision change; stale data may survive failures until TTL. |
| Category | Authenticated lists and detail; ADMIN mutations | Active and all lists cached, 30 seconds | Separate variants share the catalog revision. Detail remains a database read. Every committed catalog change advances the revision. |
| MenuItem | Authenticated list, search, category and detail; ADMIN mutations | Unfiltered active and all lists cached, 30 seconds | Full DTO includes costs and is stored separately from the public DTO. Search, category filters and detail use MySQL to avoid unbounded keys. Category and menu changes advance the shared revision. Checkout still reprices from MySQL. |
| Store | Authenticated singleton read and ADMIN update | Candidate only after measurement | Small record; invalidation after commit, including updates of public-menu snapshot. Billing snapshots must come from transactional MySQL reads. |
| Billing | Held/list/detail and checkout, hold, cancel, reprint | Do not cache | Changes frequently; correctness, idempotency, numbering and payment operations depend on current transactional state. |
| Auth | Login, refresh, logout, me and CSRF | Do not cache | Credentials, token revocation, user status and user-specific data are security-sensitive and must stay authoritative. |
| AdminRead | Users and audit logs | Do not cache | Sensitive or changing information; audit list is currently unbounded. |
| Notification | List and mark-read | Do not cache | Read state and inventory alerts change and depend on current database state. |
| AuthorizationProbe | Role test endpoints | Do not cache | Authorization must run for every request; there is no meaningful data benefit. |

Inventory and reporting repositories and views exist, but there are no corresponding production controllers beyond probes. Do not add speculative dashboard caches. If actual report endpoints become expensive, measure them, define acceptable freshness, keys including filters and role, and invalidate on all related committed writes before considering caching.

No passwords, token hashes, raw tokens, CSRF tokens, session-specific responses, or security decisions belong in a general response cache. Authorization remains outside and before any protected cache read. No cross-user cache is proposed. Null results are not cached by the current public-menu implementation.

Cached catalog lists may be stale for up to 30 seconds following a failed Redis invalidation or direct database write outside the application; in-flight reads and Redis outages require the same eventual-consistency caveats as the public menu. Redis must be private and authenticated in production because staff item DTOs include costs. The application checks authorization before serving protected catalog routes. Cache reads inside an active write transaction bypass Redis. Do not cache mutation responses or use Redis for database transactions. Exact immediate consistency is not provided by the current architecture.
