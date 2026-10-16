# HMPPS ARNS Assessment Platform API

[![repo standards badge](https://img.shields.io/badge/endpoint.svg?&style=flat&logo=github&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-arns-assessment-platform-api)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-report/hmpps-arns-assessment-platform-api)
[![Docker Repository on ghcr](https://img.shields.io/badge/ghcr.io-repository-2496ED.svg?logo=docker)](https://ghcr.io/ministryofjustice/hmpps-arns-assessment-platform-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://arns-assessment-platform-api-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html?configUrl=/v3/api-docs)

The ARNS Assessment Platform (AAP) API is the backend service for the AAP, providing a unified, event-sourced data
store for all ARNS assessment types across the justice system. It exposes a CQRS-style interface through command and
query endpoints, with full assessment versioning and an audit timeline.

## About ARNS Assessment Platform API

The API serves as the single persistent backend for assessment data, built around an event sourcing architecture
that provides:

- **Event-sourced state management**: Every change to an assessment is captured as an immutable event, enabling
  full history replay and point-in-time queries
- **CQRS interface**: Separate command and query endpoints, each accepting batches of typed operations dispatched
  through dedicated buses
- **Aggregate-based domain model**: Assessment state is derived by replaying events through typed handlers,
  with versioned snapshots for read performance
- **Assessment-agnostic data model**: A generic structure of answers, properties, and collections that supports
  any assessment type without schema changes
- **Audit timeline**: Every mutation is recorded with user attribution and timestamps

## Key Technologies

- **Language**: Kotlin
- **Framework**: Spring Boot 3 with Spring Security (OAuth2 resource server)
- **Database**: PostgreSQL (Aurora on Cloud Platform) with Flyway migrations
- **Caching**: Redis for aggregate state caching
- **Messaging**: SQS via HMPPS SQS library for audit events
- **Build**: Gradle with Kotlin DSL
- **Testing**: JUnit 5, Mockito, Testcontainers (PostgreSQL), WireMock
- **API Documentation**: SpringDoc OpenAPI (Swagger UI)

## Quick Start

### Prerequisites

- Docker and Docker Compose
- Make
- JDK 21+ (for local/IntelliJ development)

### Development Setup

The project uses containerised development with all commands available through the Makefile:

```bash
# Build the development image
make dev-build

# Start the API with hot-reload (port 8081)
# Remote debugger available on port 5006
make dev-up

# Watch for file changes and live-reload
make dev-up watch

# View all available commands
make help
```

The API will be available at http://localhost:8081 with HMPPS Auth on http://localhost:9090

### Running in IntelliJ

Start the supporting services without the API container, then run the Spring Boot application with the `dev` profile:

```bash
docker compose -f docker/docker-compose.base.yml up postgres redis hmpps-auth localstack --wait
```

### Common Development Commands

View the full list of development commands and their use by running:

```bash
make

## Project Structure

```
hmpps-arns-assessment-platform-api/
├── src/main/kotlin/.../arnsassessmentplatformapi/
│   ├── aggregate/              # Aggregate root and event handlers
│   │   └── assessment/         # AssessmentAggregate and per-event handlers
│   ├── command/                # Command types and command bus
│   │   ├── bus/                # CommandBus, retryable dispatcher
│   │   └── handler/            # One handler per command type
│   ├── query/                  # Query types and query bus
│   │   ├── bus/                # QueryBus, handler registry
│   │   └── handler/            # One handler per query type
│   ├── event/                  # Event types (sealed interface hierarchy)
│   │   └── bus/                # Event publishing
│   ├── controller/             # REST controllers
│   ├── domain/plan/            # Sentence Plan domain logic
│   ├── model/                  # Shared domain model (Value, Collection, etc.)
│   ├── persistence/            # JPA entities, repositories, projections
│   ├── service/                # Core services (Assessment, Event, Aggregate)
│   ├── config/                 # Spring configuration, security, exception handling
│   └── clock/                  # Abstracted clock for testability
├── src/main/resources/
│   └── db/migration/           # Flyway SQL migrations
├── src/test/kotlin/            # Tests (co-located by package)
├── typescript-client/          # Auto-generated TypeScript API client
├── docker/                     # Docker Compose files and support scripts
├── helm_deploy/                # Kubernetes/Helm deployment configs
└── perf/                       # Performance testing (Gatling)
```

## Event Sourcing Architecture

### Data Model

The system stores four core entity types in PostgreSQL:

| Table                    | Purpose                                                                       |
| ------------------------ | ----------------------------------------------------------------------------- |
| `assessment`             | Root entity with UUID and type (e.g. `SENTENCE_PLAN`)                         |
| `assessment_identifier`  | External identifiers linking assessments to upstream systems (e.g. OASys IDs) |
| `event`                  | Immutable append-only event log, JSONB payload with polymorphic `data_type`   |
| `aggregate`              | Versioned snapshots of derived state, rebuilt by replaying events              |
| `timeline`               | User-facing audit trail entries                                               |

### Write Path (Commands)

1. Client sends a `POST /command` with a batch of typed commands
2. The `RetryableCommandDispatcher` dispatches each command through the `CommandBus`
3. The matched `CommandHandler` validates the command, emits one or more `Event`s, and optionally creates timeline entries
4. The `AggregateState` replays events through typed `EventHandler`s to rebuild associated aggregates (e.g. `AssessmentAggregate`)
5. Events are persisted to the `event` table as immutable JSONB records
6. The updated aggregate snapshots are persisted to the `aggregate` table with an incremented version
7. Audit events are published to SQS

### Read Path (Queries)

1. Client sends a `POST /query` with a batch of typed queries
2. The `QueryBus` dispatches each query to its registered `QueryHandler`
3. Handlers read from aggregate snapshots or the event/timeline stores
4. Queries accept an optional `timestamp` parameter for point-in-time reads, replaying events up to that moment

### Event Types

Events are a sealed interface hierarchy, polymorphically serialised as JSONB:

- `AssessmentCreatedEvent` / `AssessmentAnswersUpdatedEvent` / `AssessmentPropertiesUpdatedEvent`
- `CollectionCreatedEvent` / `CollectionItemAddedEvent` / `CollectionItemRemovedEvent` / `CollectionItemReorderedEvent`
- `CollectionItemAnswersUpdatedEvent` / `CollectionItemPropertiesUpdatedEvent`
- `AssessmentRolledBackEvent` / `FormVersionUpdatedEvent`

### Command Types

Commands follow the same sealed interface pattern:

- `CreateAssessmentCommand` / `UpdateAssessmentAnswersCommand` / `UpdateAssessmentPropertiesCommand`
- `CreateCollectionCommand` / `AddCollectionItemCommand` / `RemoveCollectionItemCommand` / `ReorderCollectionItemCommand`
- `UpdateCollectionItemAnswersCommand` / `UpdateCollectionItemPropertiesCommand`
- `RollbackCommand` / `SoftDeleteCommand` / `UndeleteCommand` / `UpdateFormVersionCommand` / `CreateTimelineItemCommand`

### Query Types

- `AssessmentVersionQuery` / `CollectionQuery` / `CollectionItemQuery`
- `TimelineQuery` / `DailyVersionsQuery`
- `GetAssessmentsModifiedSinceQuery` / `GetAssessmentsSoftDeletedSinceQuery`

## Testing

### Test Types

#### Unit Tests

JUnit 5 with Mockito for isolated testing of command handlers, event handlers, query handlers, and services.

#### Integration Tests

Testcontainers-based tests that spin up a real PostgreSQL instance to verify repository queries, event replay,
and aggregate persistence end-to-end.

#### Contract Tests

WireMock-based tests for verifying interactions with external HMPPS services (Auth, etc.).

### Running Tests

```bash
make test                              # All tests in Docker
make test-targeted TESTS="*EventTest"  # Specific tests
./gradlew test                         # Locally (requires running PostgreSQL)
```

## Deployments and releases
See [ARNS Release Process](https://dsdmoj.atlassian.net/wiki/spaces/ARN/pages/6260917467/ARNS+Release+Process) for the background, release checks, feature flags and rollback.

The application is deployed to Cloud Platform environments using GitHub Actions and Helm charts.

| Trigger                                                      | Deploys to                          |
|--------------------------------------------------------------|-------------------------------------|
| Merge to `main`                                              | dev                                 |
| Run **Deploy to environment** manually (any branch)          | dev or test                         |
| Publish a **pre-release** (e.g. `v1.0.0-rc.1`)               | dev + test → preprod                |
| Publish a **release** (e.g. `v1.0.0`)                        | dev + test → preprod → prod         |

Preprod and prod need a manual approval in the release pipeline, and only change through a release.

In the release pipeline, preprod only deploys once the integration tests and e2e tests have passed against test.

### How to do a release
1. Go to **Releases → Draft a new release** and create a new `vX.Y.Z` tag targeting `main`
   (patch for fixes, minor for features, major for breaking changes).
2. Use the tag as the release title and click **Generate release notes**.
   Tick **Set as a pre-release** if it should stop at preprod.
3. Inform the Product Manager so they can raise a Change Request, and provide a link to the release notes.
4. Publish the release and follow the **Release pipeline** run in the Actions tab, approving preprod and prod when prompted.

### Rolling back
This service uses Postgres with Flyway migrations, and `helm rollback` does not undo migrations.
Only roll back if the previous version works with the current database schema; otherwise fix forward with a patch release.

### Security Scanning

- Veracode static analysis
- Trivy container image scanning
- Gitleaks secret detection

## Contributing

### Code Style

- Kotlin with ktlint enforcement
- HMPPS Kotlin patterns and conventions

### Security

Our security policy is located [here](https://github.com/ministryofjustice/hmpps-arns-assessment-platform-api/security/policy).
