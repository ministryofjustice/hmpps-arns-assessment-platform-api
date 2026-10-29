SHELL := /bin/bash
.DEFAULT_GOAL := help

include .env
export

SQLX            := sqlx migrate run --source=db/migrations
DOCKER_COMPOSE  := docker compose
IMAGE           ?= hmpps-arns-assessment-platform-api:rust
BUILD_NUMBER    ?= $(shell date +%Y-%m-%d).local.$(shell git rev-parse --short HEAD)

.PHONY: all up down build run lint test prepare image clean help

## Default target - ensure infra is ready and compile
all: up build

help:
	@echo "Available targets:"
	@echo "  make all      Start services, apply migrations, and compile"
	@echo "  make up       Start PostgreSQL, Redis, HMPPS Auth and LocalStack, then run migrations"
	@echo "  make down     Stop and remove containers"
	@echo "  make build    Compile the project (services + migrations first)"
	@echo "  make run      Run the API on port 8080 (services + migrations first)"
	@echo "  make lint     Run rustfmt and clippy (services + migrations first)"
	@echo "  make test     Run all tests (services + migrations first)"
	@echo "  make prepare  Refresh the offline query data in .sqlx after changing a query"
	@echo "  make image    Build the production Docker image, tagged $(IMAGE) (override with IMAGE=...)"
	@echo "  make clean    Stop services and remove build artefacts"

up:
	@echo "Starting PostgreSQL, Redis, HMPPS Auth and LocalStack ..."
	$(DOCKER_COMPOSE) up -d --wait postgres redis hmpps-auth localstack
	@echo "Applying pending migrations ..."
	$(SQLX)

down:
	$(DOCKER_COMPOSE) down

build: up
	cargo build

run: up
	cargo run

lint: up
	cargo fmt --check
	cargo clippy --all-targets -- -D warnings

test: up
	cargo test

prepare: up
	cargo sqlx prepare -- --all-targets

## Builds from the committed .sqlx query data, so needs no database
image:
	docker build --build-arg BUILD_NUMBER=$(BUILD_NUMBER) -t $(IMAGE) .
	@echo "Built $(IMAGE) (build $(BUILD_NUMBER))"

clean: down
	cargo clean
