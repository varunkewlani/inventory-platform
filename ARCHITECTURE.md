# Architecture

## Overview

A modular monolith: one Spring Boot application, one MySQL database, organized into feature packages that each own their own entities/repositories/services/controllers and talk to each other only through public service interfaces. See [ADR-002](ADR-002-modular-monolith.md) for why this over microservices.

```
                    ┌─────────────────┐
                    │  React (Vite)    │
                    │  frontend        │
                    └────────┬─────────┘
                             │ HTTPS / WSS
                             ▼
        ┌────────────────────────────────────────┐
        │         Spring Boot application         │
        │                                          │
        │  RateLimitFilter → JwtAuthFilter →        │
        │  TenantFilter → PermissionAspect →        │
        │  Controller → Service → Repository        │
        │                                          │
        │  auth · users · organizations · roles     │
        │  products · warehouses · inventory        │
        │  customers · orders · audit · dashboard    │
        └───┬──────────┬──────────┬─────────┬───────┘
            │           │          │          │
            ▼           ▼          ▼          ▼
        ┌───────┐  ┌────────┐ ┌────────┐ ┌──────────┐
        │ MySQL │  │ Redis  │ │ Kafka  │ │ WebSocket│
        │(InnoDB│  │(cache, │ │(order- │ │ (STOMP/  │
        │ +Flyway│  │ rate   │ │ events)│ │  SockJS) │
        │migrations)│ limit) │ │        │ │          │
        └───────┘  └────────┘ └───┬────┘ └────▲─────┘
                                   │            │
                        ┌──────────┴──────┐     │
                        ▼                 ▼     │
                 ┌─────────────┐  ┌───────────────┐
                 │ AuditWorker │  │NotificationWorker│──┘
                 │ (own consumer│  │(own consumer   │
                 │  group)      │  │ group)         │
                 └─────────────┘  └───────────────┘
```

## Layering

Controller → Service → Repository, per the assignment's suggested layering. Controllers are thin (bind/validate the request, delegate, wrap the response); all business logic — including every tenant-isolation and RBAC decision — lives in the service layer, never in the controller or the repository. This isn't Clean Architecture/Hexagonal in the strict sense (no separate domain-model layer independent of JPA entities) — a deliberate simplification for the timeline, since this project doesn't have complex domain logic that would benefit from being decoupled from persistence.

## Request pipeline

Every authenticated request passes through, in order: `RateLimitFilter` (Redis-backed, fails open) → `JwtAuthFilter` (verifies the JWT, populates Spring Security's context with a `UserPrincipal`) → `TenantFilter` (copies organization/user/role from that principal into `TenantContext`, a thread-local) → `PermissionAspect` (an AOP `@Before` advice checking `@RequiresPermission` against the caller's role) → the controller method. `TenantContext` is cleared in a `finally` block at the end of every request — load-bearing, since servlet containers reuse request-handling threads.

## Multi-tenancy

Every tenant-scoped table carries `organization_id`. The organization a request acts on is never a client-supplied parameter — it's read only from `TenantContext`, sourced only from the verified JWT. Repositories expose only tenant-scoped finder methods (`findByIdAndOrganizationId`, never a bare `findById`, for any entity implementing the `TenantOwned` marker interface). Full reasoning, including why an automatic Hibernate-filter-based approach was considered and rejected, is in [SECURITY.md](SECURITY.md).

## Concurrency

The one place the brief says correctness can't be compromised. Full reasoning in [ADR-003](ADR-003-concurrency-strategy.md); summary: every inventory quantity change is a single atomic `UPDATE ... WHERE available_quantity >= ?` statement, relying on MySQL's row lock rather than application-level locking or retries. Verified by a dedicated concurrency test firing 100 simultaneous requests at 10 units of stock through the real HTTP API against real MySQL.

## Event-driven processing

One asynchronous workflow, matching the assignment's own suggested diagram: order creation and status changes publish an `OrderEvent` (as a Spring `ApplicationEvent`, forwarded to the Kafka `order-events` topic only after the enclosing transaction commits — see the Phase 4 handoff note for why that ordering matters). Two independent Kafka consumer groups read every message: `AuditWorker` writes the audit-log entry, `NotificationWorker` pushes a WebSocket notification. Either one being slow or temporarily down doesn't affect order creation itself, which already succeeded before either worker runs.

## Caching

Redis, two use cases, each with an explicit key/TTL/invalidation strategy documented in code (`DashboardService`, `ProductServiceImpl`) and summarized in [SECURITY.md](SECURITY.md)/handoff notes — dashboard metrics (30s TTL, no write-side invalidation, an accepted staleness trade-off for a point-in-time aggregate) and per-product detail (5min TTL, evicted on write).

## Failure scenarios

| Scenario | Behavior |
|---|---|
| Database unavailable | Every request fails (there's no functioning without it — this is the system of record for every write). Connection pool (HikariCP) retries per its own configuration; no custom handling beyond that. |
| Redis unavailable | Rate limiting fails **open** (requests pass through unthrottled rather than the API going down) — see [SECURITY.md](SECURITY.md). Product/dashboard caching falls back to hitting the database directly (a cache-aside miss, not a failure) if Redis is down for a *read*; a `RedisConnectionFailureException` on a cache *write* would currently propagate as a 500 rather than being caught — a gap, not by design, noted here rather than silently left undocumented. |
| Message broker (Kafka) unavailable | Order creation and status updates still succeed — the Kafka publish only happens in an `AFTER_COMMIT` listener, decoupled from the request. Only the audit-log entry for that order and its real-time notification are delayed until Kafka recovers; they are not lost, since Kafka retains the message once the broker is back and the consumer groups resume from their last committed offset. |
| WebSocket disconnected | The client simply stops receiving live notifications; no data is lost server-side (nothing depends on a WebSocket delivery succeeding — it's push-only, fire-and-forget from the server's side). A reconnecting client currently has no way to fetch notifications it missed while disconnected — a known gap (no notification history/replay endpoint). |
| Duplicate order request | Handled via the optional `Idempotency-Key` header — a retried request with the same key returns the original order rather than creating a second one or double-reserving stock. Without a supplied key, duplicate submissions are treated as two independent orders (by design — there's no way to distinguish an intentional duplicate purchase from an accidental one without a client-supplied idempotency signal). |
| Inventory changes during checkout | Cannot happen inconsistently: the reservation check and the reservation itself are the same atomic statement (see Concurrency above) — there's no window between "check" and "reserve" where a concurrent change could be observed and then acted on. |
| Two simultaneous inventory updates | This is exactly what the concurrency test verifies: both requests race for the same row, the database's row lock serializes them, and whichever loses the race gets a clean `409 INSUFFICIENT_INVENTORY` rather than a corrupted quantity. |

## Scalability: 1,000 users → 1,000,000 users

**Stateless services, horizontal scaling.** The application already holds no server-side session state (JWT-based auth, `TenantContext` is a per-request thread-local, not shared state) — it can run as N identical instances behind a load balancer with zero code changes. The one exception is the WebSocket broker: the current in-memory `SimpleBrokerMessageHandler` only routes messages to clients connected to the *same* instance. At real scale this needs an external message broker Spring supports natively (RabbitMQ via STOMP relay, or Redis pub/sub) so a notification published on instance A reaches a client connected to instance B.

**Database.** Read replicas first — most traffic (product browsing, order history, dashboard) is reads, and Spring's `@Transactional(readOnly = true)` methods are already a clean boundary to route to a replica. Write scaling is the harder problem for any single-primary relational database; if it ever became the bottleneck, `organization_id` is the natural shard key (every tenant-scoped query already filters by it), since no query in this system joins across organizations.

**Caching.** Already in place for the highest-value cases (dashboard aggregates, product detail); would extend to product *listings* (the harder case — key composition across search/filter/sort/page combinations) and session/permission lookups if profiling showed them hot.

**CDN.** Static frontend assets (the built Vite bundle) in front of a CDN — a trivial win at real scale; the current deployment already gets this for free from Render's static-site hosting, but a dedicated CDN (CloudFront, Cloudflare) would matter more once traffic is geographically spread out.

**Message queue / async processing.** Already event-driven for orders; the same pattern (publish an event, let a worker handle the side effect) is the template for anything else that becomes a bottleneck on the request path — e.g. if audit logging or notification fan-out for *every* entity type (not just orders) became necessary, it follows the same Kafka-topic-plus-consumer-group shape already proven out here.

**Connection pooling.** HikariCP (Spring Boot's default) is already in place; its pool size is currently a default, would need tuning against actual instance count × per-instance pool size vs. MySQL's `max_connections` at real scale — too many application instances each holding a full-size pool is a classic way to exhaust the database's connection limit.

**Rate limiting.** Already Redis-backed and therefore already shared correctly across multiple instances (the counter lives in Redis, not in-process) — this one doesn't need rework to scale horizontally, just capacity planning on the Redis instance itself.

**Observability.** Structured request logging was cut for time (see [SECURITY.md](SECURITY.md), "Known limitations") — at real scale this becomes necessary, not optional, paired with centralized log aggregation, request correlation IDs, and metrics/tracing (Micrometer + Prometheus + OpenTelemetry, all mentioned as bonus items in the brief) to actually debug a multi-instance deployment.
