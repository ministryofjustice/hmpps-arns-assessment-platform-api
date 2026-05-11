FROM gradle:9-jdk25-alpine AS builder

FROM eclipse-temurin:25.0.3_9-jre-alpine AS runtime

FROM builder AS build
ARG BUILD_NUMBER
ENV BUILD_NUMBER=${BUILD_NUMBER:-1_0_0}
WORKDIR /app
ADD . .
RUN gradle --no-daemon assemble

FROM builder AS development
RUN apk add --no-cache curl
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
RUN apk add --no-cache tzdata curl
ENV TZ=Europe/London
RUN cp "/usr/share/zoneinfo/$TZ" /etc/localtime && echo "$TZ" > /etc/timezone
RUN addgroup --gid 2000 --system appgroup && \
    adduser --uid 2000 --system appuser --ingroup appgroup
WORKDIR /app
COPY --from=build --chown=appuser:appgroup /app/build/libs/hmpps-arns-assessment-platform-api*.jar /app/app.jar
COPY --from=build --chown=appuser:appgroup /app/build/libs/applicationinsights-agent*.jar /app/agent.jar
COPY --from=build --chown=appuser:appgroup /app/applicationinsights.json /app
COPY --from=build --chown=appuser:appgroup /app/applicationinsights.dev.json /app
USER 2000
ENTRYPOINT ["java", "-XX:+AlwaysActAsServerClassMachine", "-javaagent:/app/agent.jar", "-jar", "/app/app.jar"]
