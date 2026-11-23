# Infrastructure

# Infrastructure

`docker-compose.redis.yml` is an optional Redis instance for local development.
It binds only to loopback on port 6380 by default, so it does not replace the
existing Redis instance on port 6379. Start it from the project root with
`docker compose -f infra/docker-compose.redis.yml up -d`, and use
`REDIS_URL=redis://127.0.0.1:6380` in the backend environment. Choose another
host port with `REDIS_LOCAL_PORT` if 6380 is in use. Run
`docker compose -f infra/docker-compose.redis.yml exec redis redis-cli PING`
to check it. Stop it with `docker compose -f infra/docker-compose.redis.yml stop`.

This disposable local cache has no password, persistence, or TLS and is not a
production deployment. Use a private, authenticated Redis service and TLS when
required in production. Redis contains cache data only; MySQL remains the source
of truth. Never publish this local instance on a public interface.
