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

Brings up MySQL, Redis, Zookeeper, and Kafka. (Backend/frontend containers are not yet wired into `docker-compose.yml` — see the README's "Known limitations" for why, and run them locally per below in the meantime.)

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
