FROM node:24-alpine
RUN apk add --no-cache bash curl jq
WORKDIR /app
COPY docker/builder.sh .
RUN chmod +x builder.sh
USER node
ENTRYPOINT ["./builder.sh"]
