# Inventory Platform

A multi-tenant, real-time order and inventory management platform built as a technical assignment (Senior Full-Stack Engineer, take-home). Multiple organizations independently manage products, warehouses, inventory, customers, orders, users, and roles — with complete data isolation between tenants, correct inventory handling under concurrent load, and an event-driven audit/notification pipeline.

## Overview

- Multi-tenant: every organization's data is completely isolated from every other organization's.
- Role-based access control: Admin / Manager / Staff, enforced on the backend.
- The one non-negotiable correctness requirement — two customers can never both successfully buy the last unit of stock — solved with an atomic conditional database update, not application-level locking.
- Orders, once created, generate an audit trail and a real-time notification asynchronously via Kafka, without blocking the order itself.

## Technology stack

| Layer | Choice |
|---|---|
| Backend | Java 21, Spring Boot 4.1 (Web, Security, Data JPA, Validation, Actuator, WebSocket) |
| Database | MySQL 8.4 (InnoDB), schema versioned with Flyway |
| Cache / rate limiting | Redis |
| Async messaging | Kafka |
| Auth | JWT access tokens + rotating opaque refresh tokens (httpOnly cookie) |
| Frontend | React (Vite, TypeScript) |
| Testing | JUnit 5, Testcontainers (real MySQL/Redis in tests, not mocks) |
| Infra | Docker Compose |

See the [ADRs](#architecture-decision-records) for why each of these, specifically.

## Installation & running locally

See [SETUP.md](SETUP.md) for full instructions (prerequisites, environment variables, Docker Compose, running the backend/frontend directly, running tests).

Quick start:
```bash
docker compose up -d          # MySQL, Redis, Kafka
cd backend && ./mvnw spring-boot:run   # backend on :8080
cd frontend && npm install && npm run dev  # frontend on :5173
```

## Running tests

```bash
cd backend && ./mvnw test
```

Includes the mandatory concurrency test (`ConcurrencyTest`): 100 simultaneous reservation requests against 10 units of stock, run through the real HTTP API against a real MySQL instance via Testcontainers, asserting exactly 10 succeed and the rest fail cleanly with `INSUFFICIENT_INVENTORY`.

## Architecture overview

Modular monolith — one Spring Boot application organized into feature packages (`auth`, `users`, `products`, `warehouses`, `inventory`, `orders`, `customers`, `audit`, `dashboard`, `infrastructure/{kafka,redis,websocket}`), one MySQL database. Full detail, diagrams, failure-scenario handling, and the 1K→1M scaling discussion: [ARCHITECTURE.md](ARCHITECTURE.md).

## Database

ER diagram and per-table design rationale: [DATABASE.md](DATABASE.md).

## API documentation

Full endpoint reference, request/response envelope, and error codes: [API.md](API.md).

## Security

[SECURITY.md](SECURITY.md) — what's implemented against the assignment's security checklist, and what's explicitly documented as a known limitation rather than silently skipped.

## Architecture Decision Records

- [ADR-001](ADR-001-database-choice.md) — Database: MySQL vs PostgreSQL vs MongoDB
- [ADR-002](ADR-002-modular-monolith.md) — Modular monolith vs microservices
- [ADR-003](ADR-003-concurrency-strategy.md) — Concurrency strategy for inventory reservation
- [ADR-004](ADR-004-kafka-vs-simpler-broker.md) — Kafka vs a simpler broker
- [ADR-005](ADR-005-global-email-uniqueness.md) — User email: globally unique, not per-organization

## Known limitations

Stated explicitly per the assignment's own guidance ("if any requirement is unclear, state your assumptions... and proceed") — these are deliberate, time-boxed cuts, not oversights:

- **WebSocket handshake is not JWT-authenticated.** See [SECURITY.md](SECURITY.md) for the specific gap and what closing it would require.
- **Structured JSON request logging** (the spec's suggested format) was not implemented — cut for time in favor of the mandatory concurrency test and this documentation set.
- **Product *list* caching** (as opposed to single-product-by-ID, which is cached) was not implemented — the key-composition problem across search/filter/sort/page combinations was judged lower-value than dashboard/product-detail caching under the time available.
- **Docker Compose doesn't yet include the backend/frontend containers** — `docker compose up` brings up MySQL/Redis/Kafka; the application itself is run directly (`./mvnw spring-boot:run` / `npm run dev`) rather than containerized, for faster local iteration during development. Containerizing both is a mechanical next step (Dockerfiles + compose service entries), not a design gap.
- **No configurable per-organization low-stock threshold** — currently a fixed constant (5 units) shared across all tenants.
- **Actual cloud deployment** was explicitly optional per the assignment and was not attempted, in favor of the mandatory items.

## Future improvements

- JWT-authenticated WebSocket handshake (token-in-query-param or a custom `HandshakeInterceptor`).
- Structured JSON request/response logging with correlation IDs, paired with centralized log aggregation at real scale.
- Product list caching with a documented invalidation strategy across filter/sort/page combinations.
- Configurable, per-organization (or per-product) low-stock thresholds.
- Containerize the backend and frontend and wire them into `docker-compose.yml` for a true one-command `docker compose up`.
- CSRF double-submit token on the refresh-cookie flow for full protection beyond `SameSite=Lax`.
