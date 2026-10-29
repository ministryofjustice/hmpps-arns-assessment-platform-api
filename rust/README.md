# HMPPS ARNS Assessment Platform API (Rust)

A Rust port of the Kotlin/Spring Boot service in the parent directory. It serves the same API and
works on the same database, so it can replace the Kotlin service without changes to clients or data.
It follows the structure of [rust-web-app](https://github.com/LucasCairns/rust-web-app): axum
routes with an `Extension` for shared state, compile-time checked `sqlx` queries, utoipa for the
OpenAPI document, and a role-checking extractor for auth.

## Running locally

Needs Docker, Rust 1.88+ and the sqlx CLI (`cargo install sqlx-cli --no-default-features --features postgres,rustls`).

```bash
make up      # start PostgreSQL, Redis, HMPPS Auth and LocalStack, and run migrations
make run     # run the API on http://localhost:8080 (Swagger UI at /swagger-ui/)
make test    # unit tests, and API tests that each get a fresh database
make lint    # rustfmt and clippy
make help    # everything else
```

The supporting services use the same host ports as the Kotlin service's `docker/` setup, so stop
that first. `docker compose up` runs the API in a container on port 8081 instead.

Queries are checked against the database at compile time. Builds without one (Docker, CI) use the
metadata in `.sqlx` instead, so run `make prepare` and commit the result after changing a query.

## Building the production image

```bash
make image                    # tagged hmpps-arns-assessment-platform-api:rust
make image IMAGE=my/tag:1.0   # or any other tag
```

This doesn't need a database or the services running, because queries are checked against `.sqlx`.

### Using it in place of the Kotlin service

The image takes the same environment variables as the Kotlin one (including Spring's
`SPRING_DATA_REDIS_*`), listens on 8080 and has `curl` for the `/health/ping` healthcheck, so it can
replace `ghcr.io/ministryofjustice/hmpps-arns-assessment-platform-api:latest` without other changes.
In a compose stack such as the UI repo's, add an override file rather than retagging the image as
the Kotlin one, which `docker compose pull` would then replace:

```yaml
# docker-compose.rust.yml
services:
  aap-api:
    image: hmpps-arns-assessment-platform-api:rust
    pull_policy: never  # it's only available locally, so don't try to pull it
```

and include it after the other compose files (`-f docker-compose.base.yml -f docker-compose.local.yml
-f docker-compose.rust.yml`). The service adopts the database the Kotlin service migrated, so
existing data carries over, and removing the override switches back.

## How it maps to the Kotlin service

| Kotlin                                   | Rust                                                                 |
|------------------------------------------|----------------------------------------------------------------------|
| `controller/`, `config/` exception handler | `src/http/` (routes, error bodies, extractors, auth, OpenAPI)        |
| `command/` (bus, handlers, results)      | `src/command/` (`Command` enum, `handler.rs`, `bus.rs` with retries) |
| `query/` (bus, handlers, results)        | `src/query/`                                                         |
| `event/`, `aggregate/`                   | `src/event/`, `src/aggregate/` (one `match` replaces the handler classes) |
| `persistence/` entities and repositories | `src/persistence/` (entities, `PersistenceContext`, one repository module per table) |
| `service/`                               | `src/service/` (state rebuilding, audit, cache, data deletion)       |
| `hook/`, `domain/`                       | `src/hook.rs`, `src/domain/` (sentence plan, SAN and the OASys mapping) |
| `clock/`                                 | `src/clock.rs`, and `src/datetime.rs` for London-time `LocalDateTime` |
| Flyway migrations                        | `db/migrations/`, the same SQL                                       |

Spring's dependency injection becomes plain functions taking a connection, so the caller decides
the transaction: commands run in one read-write transaction (retried on concurrent updates),
queries in one read-only transaction.

## Configuration

The same environment variables as the Kotlin service, so the Helm chart works unchanged.

| Variable                                   | Default                       |
|--------------------------------------------|-------------------------------|
| `SERVER_PORT`                              | `8080`                        |
| `DATABASE_ENDPOINT`                        | `localhost:5432`              |
| `DATABASE_NAME`                            | `postgres`                    |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD`  | `root` / `dev`                |
| `DATABASE_SCHEMA_NAME`                     | `assessment-platform`         |
| `DATABASE_URL`                             | Overrides the above when set  |
| `DATABASE_POOL_SIZE`                       | `20`                          |
| `HMPPS_AUTH_URL`                           | `http://localhost:8090/auth`  |
| `CACHE_REDIS_HOST` / `CACHE_REDIS_PORT`    | `localhost` / `6379` (Spring's `SPRING_DATA_REDIS_*` take precedence, as they did) |
| `CACHE_REDIS_AUTH_TOKEN`                   |                               |
| `CACHE_REDIS_SSL_ENABLED`                  | `false`                       |
| `CACHE_REDIS_TIMEOUT` / `CACHE_REDIS_CONNECT_TIMEOUT` | `5s` / `2s`        |
| `HMPPS_SQS_QUEUES_AUDIT_QUEUE_NAME`        | Audit events are only logged when unset |
| `HMPPS_SQS_PROVIDER` / `HMPPS_SQS_LOCALSTACK_URL` | `localstack` sends to LocalStack |
| `QUERY_MAX_LOOKBACK_DAYS`                  | `1` (`0` disables the check)  |
| `TZ`                                       | `Europe/London`               |

`TZ` matters: the API's timestamps have no offset and, as with the Kotlin service, are London time.

## Taking over from the Kotlin service

On first start against a database Flyway has migrated, the service records the migrations in
`flyway_schema_history` as already applied in sqlx's `_sqlx_migrations`, then runs only newer ones.
Data written by either service reads the same in the other. Rolling back to the Kotlin service is
safe for a database Flyway created, as long as no migration has been added here since: Flyway
ignores `_sqlx_migrations`, but would try to create the schema again in a database it has no
history for.

Not ported: Application Insights telemetry and the Glowroot agent. Logs go to stdout via `tracing`
(`RUST_LOG` sets the level).

## Differences from the Kotlin service

Requests and responses match the Kotlin service, except for these deliberate changes:

- Requests the Kotlin service failed with a 500 are 400s here: unparseable `@index` placeholders
  or UUIDs, an invalid `backdateTo`, an out of range timeline page or result limit, and invalid
  path or query parameters.
- Unauthenticated requests get the standard JSON error body rather than an empty 401.
- Commands are retried on unique constraint conflicts (two requests creating the same user, or
  writing the same assessment at once) as well as optimistic lock failures.
- Commands are audited after their transaction commits rather than just before.
- Data deletion responses describe a replay failure as `{eventUuid, eventName, handlerName,
  message, developerMessage, statusCode}` instead of a serialised exception with a stack trace,
  and key `rebuiltState` by `AssessmentAggregate` instead of the Kotlin class name.
- A goal whose `status` isn't a single value is left alone when starting a new period of
  supervision, rather than failing the request.
- JSON properties may come in a different order (the order has no meaning in JSON).

Kept as the Kotlin service did it, but probably worth revisiting:

- Updating a timeline entry through data deletion moves it to the end of the timeline, because
  timeline entries are always given new positions when saved.
- Events and timeline entries are inserted one row at a time rather than in batches.
