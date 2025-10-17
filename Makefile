SHELL = '/bin/bash'
LOCAL_COMPOSE_FILES = -f docker-compose.yml -f docker-compose.local.yml
DEV_COMPOSE_FILES = -f docker-compose.yml -f docker-compose.local.yml -f docker-compose.dev.yml
PROJECT_NAME = hmpps-arns-assessment-platform
SERVICE_NAME = api

export COMPOSE_PROJECT_NAME=${PROJECT_NAME}

default: help

help: ## The help text you're reading.
	@grep --no-filename -E '^[0-9a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

up: ## Starts/restarts the API in a production container.
	docker compose ${LOCAL_COMPOSE_FILES} down ${SERVICE_NAME}
	docker compose ${LOCAL_COMPOSE_FILES} up ${SERVICE_NAME} --wait --no-recreate

down: ## Stops and removes all containers in the project.
	docker compose ${DEV_COMPOSE_FILES} down
	docker compose ${LOCAL_COMPOSE_FILES} down

build-api: ## Builds a production image of the API.
	docker compose build ${SERVICE_NAME}

dev-up: ## Starts/restarts the API in a development container. A remote debugger can be attached on port 5005.
	docker compose down ${SERVICE_NAME}
	docker compose ${DEV_COMPOSE_FILES} up --wait --no-recreate ${SERVICE_NAME}

dev-build: ## Builds a development image of the API.
	docker compose ${DEV_COMPOSE_FILES} build ${SERVICE_NAME}

dev-down: ## Stops and removes the API container.
	docker compose down ${SERVICE_NAME}

rebuild: ## Re-builds and live-reloads the API.
	docker compose ${DEV_COMPOSE_FILES} exec ${SERVICE_NAME} gradle compileKotlin --parallel --build-cache --configuration-cache

watch: ## Watches for file changes and live-reloads the API. To be used in conjunction with dev-up e.g. "make dev-up watch"
	docker compose ${DEV_COMPOSE_FILES} exec ${SERVICE_NAME} gradle compileKotlin --continuous --parallel --build-cache --configuration-cache

test: ## Runs all the test suites.
	docker compose ${DEV_COMPOSE_FILES} exec \
	   --env HMPPS_AUTH_URL=http://localhost:9090/auth \
      ${SERVICE_NAME} \
      gradle test --parallel

test-unit: ## Runs the unit test suite.
	docker compose ${DEV_COMPOSE_FILES} exec ${SERVICE_NAME} gradle unitTests --parallel

test-integration: ## Runs the integration test suite.
	docker compose ${DEV_COMPOSE_FILES} exec ${SERVICE_NAME} gradle integrationTests --parallel

lint: ## Runs the Kotlin linter.
	docker compose ${DEV_COMPOSE_FILES} exec ${SERVICE_NAME} gradle ktlintCheck --parallel

lint-fix: ## Runs the Kotlin linter and auto-fixes.
	docker compose ${DEV_COMPOSE_FILES} exec ${SERVICE_NAME} gradle ktlintFormat --parallel

lint-baseline: ## Generate a baseline file, ignoring all existing code smells.
	docker compose ${DEV_COMPOSE_FILES} exec ${SERVICE_NAME} gradle --parallel

update: ## Downloads the latest versions of containers.
	docker compose pull

# Client builder config
SPEC_URL ?= http://api:8080/v3/api-docs
CACHE_DIR := typescript-client/.cache
SPEC_JSON := $(CACHE_DIR)/spec.json
SPEC_SORT := $(CACHE_DIR)/spec.sorted.json
HASH_FILE := $(CACHE_DIR)/spec.sha256
OUT_DIR := typescript-client/src
NETWORK ?= hmpps-arns-assessment-platform_hmpps

# Client builder util image config
DOCKERFILE ?= ./typescript-client/Dockerfile
BUILD_CTX  ?= ./typescript-client
CLIENT_BUILDER_DOCKER := client-builder:latest

.PHONY: build-util-docker-image
build-util-docker-image:
	@echo "🐳 Building $(CLIENT_BUILDER_DOCKER)"
	@docker build -t $(CLIENT_BUILDER_DOCKER) -f $(DOCKERFILE) $(BUILD_CTX)

.PHONY: client clean

$(CACHE_DIR):
	mkdir -p $@
# Step 1: fetch latest spec into the cache
$(SPEC_JSON): | $(CACHE_DIR) build-util-docker-image
	@echo "📥 Fetching OpenAPI spec from $(SPEC_URL)"
	@docker run --rm --network $(NETWORK) \
        -v $(abspath $(CACHE_DIR)):/cache \
        $(CLIENT_BUILDER_DOCKER) \
        sh -c 'set -e; curl -sSL "$(SPEC_URL)" -o /cache/spec.json'

# Generate the client code
.PHONY: build-client
build-client: | build-util-docker-image $(SPEC_JSON)
	@docker run --rm -v $(abspath $(CACHE_DIR)):/cache \
        $(CLIENT_BUILDER_DOCKER) jq -S . /cache/spec.json > $(SPEC_SORT)
	@NEW_HASH=$$(docker run --rm $(CLIENT_BUILDER_DOCKER) bash -c "sha256sum" < $(SPEC_SORT) | cut -d' ' -f1); \
        OLD_HASH=$$(cat $(HASH_FILE) 2>/dev/null || true); \
        if [ "$$NEW_HASH" = "$$OLD_HASH" ]; then \
            echo '✅ Spec unchanged, skipping codegen'; \
            exit 0; \
        fi; \
        echo "⬆️ Spec updated! Old hash: $$OLD_HASH"; \
        echo "🔄 New hash: $$NEW_HASH"; \
        echo "$$NEW_HASH" > $(HASH_FILE); \
        echo '⚙️ Generating client...'; \
        docker run --rm --network $(NETWORK) \
        -v $(abspath $(CACHE_DIR)):/home/node/app/cache \
        -v $(abspath $(OUT_DIR)):/home/node/app/src/client \
        $(CLIENT_BUILDER_DOCKER) \
        bash -lc 'set -e; \
            npx @hey-api/openapi-ts -i /home/node/app/cache/spec.json -o /home/node/app/build/ && \
            mkdir -p /home/node/app/src/client && rm -rf /home/node/app/src/client/* && \
            cp -r /home/node/app/build/* /home/node/app/src/client/'; \
        echo '✅ Done! The output is in ./$(OUT_DIR)'

clean: ## Stops and removes all project containers. Deletes local build/cache directories.
	docker compose down
	rm -rf .gradle build $(CACHE_DIR) $(OUT_DIR)
