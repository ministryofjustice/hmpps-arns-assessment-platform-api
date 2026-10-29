# syntax=docker/dockerfile:1
FROM rust:1.99-bookworm AS build
ARG BUILD_NUMBER
ENV BUILD_NUMBER=${BUILD_NUMBER:-1_0_0}
# Queries are checked against the metadata in .sqlx, so no database is needed to build
ENV SQLX_OFFLINE=true
WORKDIR /app
COPY . .
RUN --mount=type=cache,target=/usr/local/cargo/registry \
    --mount=type=cache,target=/app/target \
    cargo build --release --locked && \
    cp target/release/hmpps-arns-assessment-platform-api /app/hmpps-arns-assessment-platform-api

FROM debian:bookworm-slim AS production
LABEL maintainer="HMPPS Digital Studio <info@digital.justice.gov.uk>"
ARG BUILD_NUMBER
ENV BUILD_NUMBER=${BUILD_NUMBER:-1_0_0}
# Local date-times in the API are London time, as they were from the Kotlin service
ENV TZ=Europe/London
# curl is used by the docker-compose healthchecks
RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends ca-certificates curl && \
    rm -rf /var/lib/apt/lists/* && \
    groupadd --gid 2000 appgroup && \
    useradd --uid 2000 --gid appgroup --no-create-home appuser
COPY --from=build --chown=appuser:appgroup /app/hmpps-arns-assessment-platform-api /app/app
USER 2000
EXPOSE 8080
ENTRYPOINT ["/app/app"]
