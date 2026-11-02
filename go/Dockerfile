# syntax=docker/dockerfile:1
FROM golang:1.27-bookworm AS build
RUN adduser \
    --disabled-password \
    --gecos "" \
    --home "/nonexistent" \
    --shell "/sbin/nologin" \
    --no-create-home \
    --uid 65532 \
    app
ARG TARGETARCH
WORKDIR /app
COPY go.mod go.sum ./
RUN --mount=type=cache,target=/go/pkg/mod go mod download
COPY cmd cmd
COPY internal internal
RUN --mount=type=cache,target=/go/pkg/mod \
    --mount=type=cache,target=/root/.cache/go-build \
    CGO_ENABLED=0 GOOS=linux GOARCH=${TARGETARCH} \
    go build -trimpath -ldflags="-s -w" -o /app/hmpps-arns-assessment-platform-api ./cmd/api

FROM scratch AS production
LABEL maintainer="HMPPS Digital Studio <info@digital.justice.gov.uk>"
ARG BUILD_NUMBER
ENV BUILD_NUMBER=${BUILD_NUMBER:-1_0_0}
# Local date-times in the API are London time, as they were from the Kotlin service
ENV TZ=Europe/London
COPY --from=build /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/
COPY --from=build /usr/share/zoneinfo /usr/share/zoneinfo
COPY --from=build /etc/passwd /etc/passwd
COPY --from=build /etc/group /etc/group
COPY --from=build /app/hmpps-arns-assessment-platform-api /app/app
USER app:app
EXPOSE 8080
# There's no curl in the image, so the binary checks itself
HEALTHCHECK --interval=5s --timeout=5s CMD ["/app/app", "healthcheck"]
ENTRYPOINT ["/app/app"]
