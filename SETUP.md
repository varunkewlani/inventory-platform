# Setup

## Prerequisites
- Java 21, Maven (or use the included `./mvnw` wrapper — no local Maven install needed)
- Node.js 20+ and npm
- Docker + Docker Compose

## Environment variables

Copy `.env.example` to `.env` and adjust as needed — every value has a working local-dev default, so this step is optional for local development but required before deploying anywhere real (the JWT secret in particular must be changed).

## Run everything via Docker Compose

```bash
docker compose up -d
```

Brings up MySQL, Redis, Zookeeper, Kafka, the backend, and the frontend — the complete stack in one command.

## Run the backend locally

```bash
cd backend
DB_HOST=localhost DB_PORT=3306 DB_NAME=inventory_platform DB_USERNAME=inventory DB_PASSWORD=inventory \
REDIS_HOST=localhost REDIS_PORT=6379 \
KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
CORS_ALLOWED_ORIGINS=http://localhost:5173 \
./mvnw spring-boot:run
```

Flyway migrations run automatically on startup. The app listens on `:8080`.

## Run the frontend locally

```bash
cd frontend
npm install
npm run dev
```

Listens on `:5173`, proxying API calls to `http://localhost:8080/api/v1` (see `VITE_API_BASE_URL` in `frontend/.env.example`).

## Run tests

```bash
cd backend
./mvnw test
```

Integration tests (including the mandatory concurrency test) use Testcontainers and need Docker running — they spin up real, disposable MySQL and Redis containers per test class, not mocks.

## Verify it's up

```bash
curl http://localhost:8080/actuator/health
```

## Deploying to a managed host (e.g. Render + a managed MySQL/Redis)

Beyond the vars in `.env.example`, a couple of extra env vars exist specifically for hosts where the database/cache require TLS, or where the frontend and backend are deployed on different domains:

| Variable | Default | When to change it |
|---|---|---|
| `DB_USE_SSL`, `DB_REQUIRE_SSL` | `false` | Set both to `true` for a managed MySQL that requires TLS (e.g. Aiven). |
| `DB_VERIFY_SERVER_CERT` | `false` | Leave `false` unless you've bundled the provider's CA cert — encrypts the connection without verifying its certificate chain. |
| `REDIS_SSL_ENABLED` | `false` | Set `true` for a managed Redis that requires TLS (e.g. Upstash). |
| `COOKIE_SECURE` | `false` | Set `true` once served over HTTPS — required for `COOKIE_SAME_SITE=None` to work at all (browsers reject `SameSite=None` without `Secure`). |
| `COOKIE_SAME_SITE` | `Lax` | `Lax` works when frontend and backend share a site (localhost, or one custom domain). Set `None` when they're on different sites (e.g. two separate `*.onrender.com` subdomains — the Public Suffix List treats those as cross-site) — otherwise the refresh cookie is silently dropped and login/refresh breaks. |
| `KAFKA_LISTENER_AUTO_STARTUP` | `true` | Set `false` if no Kafka broker is deployed — without this, the `@KafkaListener` consumers retry-loop against an unreachable broker, which on a constrained host starves other request threads. |
| `KAFKA_TOPIC_PROVISIONING_ENABLED` | `true` | Set `false` alongside the above — otherwise Spring Kafka's `AdminClient` blocks application startup for minutes trying to provision the topic against a broker that isn't there. |
| `APP_KAFKA_PUBLISHING_ENABLED` | `true` | Set `false` alongside the above two if no broker is deployed at all, so order-creation doesn't spend time on producer sends that can't succeed. |
