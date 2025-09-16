SHELL = '/bin/bash'
LOCAL_COMPOSE_FILES = -f docker-compose.yml -f docker-compose.local.yml
DEV_COMPOSE_FILES = -f docker-compose.yml -f docker-compose.local.yml -f docker-compose.dev.yml
PROJECT_NAME = hmpps-arns-assessment-platform

export COMPOSE_PROJECT_NAME=${PROJECT_NAME}

default: help

help: ## The help text you're reading.
	@grep --no-filename -E '^[0-9a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

up: ## Starts/restarts the API in a production container.
	docker compose ${LOCAL_COMPOSE_FILES} down hmpps-arns-assessment-platform-api
	docker compose ${LOCAL_COMPOSE_FILES} up hmpps-arns-assessment-platform-api --wait --no-recreate

down: ## Stops and removes all containers in the project.
	docker compose ${DEV_COMPOSE_FILES} down
	docker compose ${LOCAL_COMPOSE_FILES} down

build-api: ## Builds a production image of the API.
	docker compose build hmpps-arns-assessment-platform-api

dev-up: ## Starts/restarts the API in a development container. A remote debugger can be attached on port 5005.
	docker compose down hmpps-arns-assessment-platform-api
	docker compose ${DEV_COMPOSE_FILES} up --wait --no-recreate hmpps-arns-assessment-platform-api

dev-build: ## Builds a development image of the API.
	docker compose ${DEV_COMPOSE_FILES} build hmpps-arns-assessment-platform-api

dev-down: ## Stops and removes the API container.
	docker compose down hmpps-arns-assessment-platform-api

rebuild: ## Re-builds and live-reloads the API.
	docker compose ${DEV_COMPOSE_FILES} exec hmpps-arns-assessment-platform-api gradle compileKotlin --parallel --build-cache --configuration-cache

watch: ## Watches for file changes and live-reloads the API. To be used in conjunction with dev-up e.g. "make dev-up watch"
	docker compose ${DEV_COMPOSE_FILES} exec hmpps-arns-assessment-platform-api gradle compileKotlin --continuous --parallel --build-cache --configuration-cache

test: ## Runs all the test suites.
	docker compose ${DEV_COMPOSE_FILES} exec \
	   --env HMPPS_AUTH_URL=http://localhost:9090/auth \
      hmpps-arns-assessment-platform-api \
      gradle test --parallel

test-unit: ## Runs the unit test suite.
	docker compose ${DEV_COMPOSE_FILES} exec hmpps-arns-assessment-platform-api gradle unitTests --parallel

test-integration: ## Runs the integration test suite.
	docker compose ${DEV_COMPOSE_FILES} exec hmpps-arns-assessment-platform-api gradle integrationTests --parallel

lint: ## Runs the Kotlin linter.
	docker compose ${DEV_COMPOSE_FILES} exec hmpps-arns-assessment-platform-api gradle ktlintCheck --parallel

lint-fix: ## Runs the Kotlin linter and auto-fixes.
	docker compose ${DEV_COMPOSE_FILES} exec hmpps-arns-assessment-platform-api gradle ktlintFormat --parallel

lint-baseline: ## Generate a baseline file, ignoring all existing code smells.
	docker compose ${DEV_COMPOSE_FILES} exec hmpps-arns-assessment-platform-api gradle --parallel

clean: ## Stops and removes all project containers. Deletes local build/cache directories.
	docker compose down
	rm -rf .gradle build

update: ## Downloads the latest versions of containers.
	docker compose pull
