# Security

What's implemented, against the assignment's security checklist (§20).

## Password hashing
BCrypt (`BCryptPasswordEncoder`), via Spring Security. Passwords are never logged, never returned in any API response, never stored anywhere but the hash.

## JWT validation
Access tokens are short-lived (15 minutes, configurable via `JWT_ACCESS_TTL_MINUTES`), signed HS256 with a server-side secret (`JWT_SECRET`, required to be long/random in any real deployment — the checked-in default is explicitly marked dev-only). `JwtAuthFilter` verifies the signature and expiry on every request; an invalid or expired token is treated as "not authenticated," not rejected with a stack trace.

## Refresh token security
Refresh tokens are **not** JWTs — they're opaque, cryptographically random (32 bytes via `SecureRandom`), server-tracked, and revocable, delivered as an **httpOnly** cookie scoped to `/api/v1/auth` (never readable from JavaScript, so an XSS payload can't exfiltrate it). Only a SHA-256 hash is stored in the database; a database leak doesn't hand out usable sessions. Every refresh **rotates**: the presented token is immediately marked revoked and a new one issued. Presenting an already-revoked token is treated as a signal of theft/replay and revokes **every** active session for that user, not just the one being reused.

## RBAC
Three roles (`ADMIN`, `MANAGER`, `STAFF`) map to a fixed permission set (`RolePermissions`), checked via `@RequiresPermission(...)` + an AOP aspect (`PermissionAspect`) on every write and most read endpoints — enforced entirely on the backend; the frontend hiding a button is a UX nicety, never the actual control. One case needed a manual check beyond the annotation: cancelling an order needs the narrower `ORDER_CANCEL` permission (which Staff doesn't have) while general status updates only need `ORDER_UPDATE_STATUS` (which Staff does) — `@RequiresPermission` can't inspect the request body to tell those apart, so `OrderServiceImpl.updateStatus()` adds one explicit `RolePermissions.has(...)` check for the `CANCELLED` case specifically.

## Tenant isolation
Every tenant-scoped table carries `organization_id`. The organization a request acts on is never a client-supplied parameter — it comes only from `TenantContext`, a thread-local populated by `TenantFilter` directly from the verified JWT's claims, immediately after authentication and cleared at the end of every request. Every tenant-scoped repository exposes only `findByIdAndOrganizationId`/`findAllByOrganizationId`-style methods (never a bare `findById`), and every entity implements a `TenantOwned` marker as a visible, review-time signal of which entities this discipline applies to. (An automatic, Hibernate-`@Filter`-based approach was considered and rejected — see the Phase 1 handoff note — in favor of this simpler, more predictable explicit-parameter approach for a security-critical invariant.) Verified directly, repeatedly, throughout development: a second organization registered mid-testing consistently saw zero rows from the first organization's data, and guessing another organization's numeric ID returns a clean 404, not a leak.

## Request validation
Every request body is validated with Jakarta Bean Validation (`@Valid` + constraint annotations) before it reaches any service logic; a failed validation returns `400 VALIDATION_ERROR` with per-field messages, never a raw stack trace.

## SQL injection
All data access goes through Spring Data JPA (parameterized queries / JPQL), including every custom `@Query`. No string-concatenated SQL anywhere in the codebase.

## XSS
The API is JSON-only (no server-rendered HTML), which removes the classic reflected/stored-XSS-via-template vector entirely. The refresh token — the one piece of security-sensitive state a script could otherwise read — is httpOnly specifically so an XSS payload elsewhere (e.g. a compromised frontend dependency) can't steal it.

## CORS
Explicit allow-list (`CORS_ALLOWED_ORIGINS`, defaults to the local frontend dev origin only) with `allowCredentials(true)` (required for the refresh cookie) — never a wildcard `*` origin, which is incompatible with credentialed requests anyway.

## Rate limiting
Redis-backed, fixed-window, 100 requests/minute per client IP, applied before authentication so it also covers unauthenticated brute-force attempts against `/auth/login` and `/auth/register`. **Fails open**: if Redis is unreachable, requests are allowed through rather than the entire API returning 500 — a rate limiter that takes the whole system down when its own dependency hiccups is worse than no rate limiter. This is this app's answer to the "Redis unavailable" failure scenario, specifically for this feature.

## Sensitive information handling
Passwords, token hashes, and the JWT signing secret never appear in logs. Error responses return a generic message for unexpected failures (`INTERNAL_ERROR`) — the real exception and stack trace are logged server-side only, never returned to the client.

## Environment variables / secrets
All configuration that differs between environments (DB credentials, JWT secret, CORS origins, Kafka/Redis hosts) is read from environment variables with local-dev-only defaults, never hardcoded. `.env` is gitignored; `.env.example` documents every variable with no real values. Secrets have never been committed — verified via `git status`/`git diff` review before every commit throughout this project.

## Secure HTTP headers
`X-Content-Type-Options: nosniff` and `X-Frame-Options: DENY` set explicitly; Spring Security's other framework defaults (safe defaults for cache-control on sensitive responses, etc.) are relied upon rather than re-implemented.

## Known limitations (stated explicitly, not hidden)
- **WebSocket handshake is not JWT-authenticated.** Browser WebSocket/SockJS can't attach a custom `Authorization` header at handshake time, and implementing token-in-query-param or a custom handshake interceptor was judged not worth the remaining time against mandatory-weighted items. Anyone who can guess/enumerate an organization ID could currently subscribe to that organization's notification topic. This needs fixing before any real deployment — see [ARCHITECTURE.md](ARCHITECTURE.md#failure-scenarios) for how this would be closed.
- **No CSRF token on the refresh-cookie flow.** Mitigated by `SameSite=Lax` on the refresh cookie (blocks the cookie being sent on cross-site `POST`s) and by the refresh endpoint being idempotent-ish and low-value to an attacker (it can't be used to perform a state-changing action beyond issuing new tokens tied to a token the attacker doesn't have), but a double-submit CSRF token would be the more complete fix for a production deployment.
- **Structured JSON request logging** (the spec's example format: timestamp/level/requestId/orgId/userId/method/path/duration) was cut for time and not implemented — noted here rather than silently skipped.
