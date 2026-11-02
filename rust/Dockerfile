# syntax=docker/dockerfile:1
# Built on Alpine for a fully static (musl) binary that runs on scratch. TLS is rustls and the time
# zone data is compiled in (chrono-tz), so only the CA certificates are needed alongside it.
FROM rust:1.99-alpine AS build
# curl is for utoipa-swagger-ui, which downloads Swagger UI at build time
RUN apk add --no-cache musl-dev cmake make perl curl ca-certificates && \
    adduser \
    -D \
    -g "" \
    -h "/nonexistent" \
    -s "/sbin/nologin" \
    -H \
    -u 65532 \
    app
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

FROM scratch AS production
LABEL maintainer="HMPPS Digital Studio <info@digital.justice.gov.uk>"
ARG BUILD_NUMBER
ENV BUILD_NUMBER=${BUILD_NUMBER:-1_0_0}
# Local date-times in the API are London time, as they were from the Kotlin service
ENV TZ=Europe/London
COPY --from=build /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/
COPY --from=build /etc/passwd /etc/passwd
COPY --from=build /etc/group /etc/group
COPY --from=build /app/hmpps-arns-assessment-platform-api /app/app
USER app:app
EXPOSE 8080
# There's no curl in the image, so the binary checks itself
HEALTHCHECK --interval=5s --timeout=5s CMD ["/app/app", "healthcheck"]
ENTRYPOINT ["/app/app"]
