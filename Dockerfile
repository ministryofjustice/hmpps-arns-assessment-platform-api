FROM gradle:9-jdk25-alpine AS builder

FROM ghcr.io/ministryofjustice/hmpps-eclipse-temurin:25-jre-jammy AS runtime

FROM builder AS build
ARG BUILD_NUMBER
ENV BUILD_NUMBER=${BUILD_NUMBER:-1_0_0}
WORKDIR /app
ADD . .
RUN gradle --no-daemon assemble

FROM builder AS development
RUN apk upgrade --no-cache && \
    apk add --no-cache curl
WORKDIR /
RUN curl -L https://github.com/glowroot/glowroot/releases/download/v0.14.6/glowroot-0.14.6-dist.zip -o glowroot.zip
RUN unzip glowroot.zip
RUN rm glowroot.zip
ADD docker/glowroot /glowroot/
RUN javac /glowroot/GlowrootDummy.java -d /glowroot/build
WORKDIR /app

FROM runtime AS production
LABEL maintainer="HMPPS Digital Studio <info@digital.justice.gov.uk>"
ARG BUILD_NUMBER
ENV BUILD_NUMBER=${BUILD_NUMBER:-1_0_0}
# curl is used by the docker-compose healthchecks
USER root
RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*
COPY --from=build --chown=appuser:appgroup /app/build/libs/hmpps-arns-assessment-platform-api*.jar /app/app.jar
COPY --from=build --chown=appuser:appgroup /app/build/libs/applicationinsights-agent*.jar /app/agent.jar
COPY --from=build --chown=appuser:appgroup /app/applicationinsights.json /app
COPY --from=build --chown=appuser:appgroup /app/applicationinsights.dev.json /app
USER 2000
ENTRYPOINT ["java", "-XX:+AlwaysActAsServerClassMachine", "-javaagent:/app/agent.jar", "-jar", "/app/app.jar"]
