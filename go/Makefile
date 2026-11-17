SHELL := /bin/bash
.DEFAULT_GOAL := help

include .env
export

DOCKER_COMPOSE  := docker compose
IMAGE           ?= hmpps-arns-assessment-platform-api:go
BUILD_NUMBER    ?= $(shell date +%Y-%m-%d).local.$(shell git rev-parse --short HEAD)

.PHONY: up down build run lint test image clean help

help:
	@echo "Available targets:"
	@echo "  make up     Start PostgreSQL, Redis, HMPPS Auth and LocalStack"
	@echo "  make down   Stop and remove containers"
	@echo "  make build  Compile the service"
	@echo "  make run    Run the API on port 8080 (services first)"
	@echo "  make lint   Check formatting and run go vet"
	@echo "  make test   Run all tests; the API tests use the PostgreSQL from 'make up'"
	@echo "  make image  Build the production Docker image, tagged $(IMAGE) (override with IMAGE=...)"
	@echo "  make clean  Stop services and remove build artefacts"

up:
	@echo "Starting PostgreSQL, Redis, HMPPS Auth and LocalStack ..."
	$(DOCKER_COMPOSE) up -d --wait postgres redis hmpps-auth localstack

down:
	$(DOCKER_COMPOSE) down

build:
	go build -o bin/api ./cmd/api

run: up
	go run ./cmd/api

lint:
	@unformatted=$$(gofmt -l .); if [ -n "$$unformatted" ]; then echo "Not formatted: $$unformatted"; exit 1; fi
	go vet ./...

test: up
	go test -race ./...

image:
	docker build --build-arg BUILD_NUMBER=$(BUILD_NUMBER) -t $(IMAGE) -f ../Dockerfile ..
	@echo "Built $(IMAGE) (build $(BUILD_NUMBER))"

clean: down
	rm -rf bin
	go clean -testcache
