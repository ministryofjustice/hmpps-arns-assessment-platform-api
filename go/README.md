# HMPPS ARNS Assessment Platform API (Go)

A Go port of the Kotlin/Spring Boot service in the parent directory. Both serve the same API and
work on the same database, so either can replace the other without changes to clients or data.

It uses the standard library's `net/http` routing and `log/slog`, `pgx` for PostgreSQL,
`go-redis`, the AWS SDK for the audit queue, and `golang-jwt` with `keyfunc` for HMPPS Auth tokens.

## Running locally

Needs Docker and Go 1.25+.

```bash
make up      # start PostgreSQL, Redis, HMPPS Auth and LocalStack
make run     # run the API on http://localhost:8080 (Swagger UI at /swagger-ui/index.html)
make test    # unit tests, and API tests that each create a fresh database
make lint    # gofmt and go vet
make help    # everything else
```

The supporting services use the same host ports as the Kotlin service's `docker/` setup, so stop
that first. The API tests create databases on `TEST_DATABASE_URL`, and are skipped if it can't be
reached.

## Building the production image

```bash
make image                    # tagged hmpps-arns-assessment-platform-api:go
make image IMAGE=my/tag:1.0   # or any other tag
```

### Using it in place of the Kotlin service

The image takes the same environment variables as the Kotlin one (including Spring's
`SPRING_DATA_REDIS_*`) and listens on 8080. It's built `FROM scratch`, so there's no shell or
`curl`: healthchecks run `/app/app healthcheck` instead, which pings `/health/ping`. In a compose
stack such as the UI repo's, add an override file rather than retagging the image as the Kotlin
one, which `docker compose pull` would then replace:

```yaml
# docker-compose.go.yml
services:
  aap-api:
    image: hmpps-arns-assessment-platform-api:go
    pull_policy: never  # it's only available locally, so don't try to pull it
    healthcheck:
      test: ["CMD", "/app/app", "healthcheck"]  # replaces the curl check, as there's no curl
```

and include it after the other compose files. Removing the override switches back.

## How it maps to the Kotlin service

| Kotlin                                     | Go                                                                    |
|--------------------------------------------|-----------------------------------------------------------------------|
| `controller/`, `config/`, security         | `internal/httpapi` (routes, error bodies, auth, health, OpenAPI)      |
| `command/`                                 | `internal/command` (types, handlers, retrying transactional dispatcher, hooks) |
| `query/`                                   | `internal/query`                                                      |
| `event/`                                   | `internal/event`                                                      |
| `aggregate/`                               | `internal/aggregate` (one switch replaces the handler classes)        |
| `service/StateService`, `PersistenceContext`, event bus | `internal/engine`                                        |
| `persistence/` repositories                | `internal/store`, and `internal/entity` for the rows                  |
| `domain/san`, `domain/plan`                | `internal/san`, `internal/plan`                                       |
| `service/` audit, cache, data deletion     | `internal/audit`, `internal/cache`, `internal/datadeletion`           |
| `clock/`                                   | `internal/clock`, and `internal/datetime` for London-time `LocalDateTime` |
| Flyway migrations                          | `internal/db/migrations`, the same SQL                                |

A few small packages fill gaps in Go's standard library: `internal/omap` keeps answers in insertion
order (the Kotlin service's `LinkedHashMap`s), and `internal/jsonx` handles types identified by a
`type` property and rejects missing required fields as Jackson did.

The OpenAPI document is `internal/httpapi/openapi.json`, generated from the request and response
types of a Rust port of the service (the API is the same), with this service's build number filled
in when it's served. Update it by hand when the API changes.

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
| `LOG_LEVEL`                                | `INFO` (or `DEBUG`, `WARN`, `ERROR`) |

`TZ` matters: the API's timestamps have no offset and, as with the Kotlin service, are London time.

## Taking over from the Kotlin service

Migrations are recorded in `schema_migrations`. The first time the service starts against a
database the Kotlin service (Flyway) migrated, it records the migrations Flyway applied as already
applied, so they aren't run again. It does the same for a database migrated by the Rust port
(sqlx).

Not ported: Application Insights telemetry and the Glowroot agent. Logs go to stdout.

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
- Updates to the Redis cache are applied one at a time in the order they were made, so an eviction
  can never land after a later write of the same assessment.
- JSON properties may come in a different order (the order has no meaning in JSON).

Kept as the Kotlin service did it, but probably worth revisiting:

- Updating a timeline entry through data deletion moves it to the end of the timeline, because
  timeline entries are always given new positions when saved.
- Events and timeline entries are inserted one row at a time rather than in batches.
