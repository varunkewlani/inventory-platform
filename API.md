# API Reference

Base path: `/api/v1`. All endpoints return JSON in the envelope described below. All endpoints except `/auth/register` and `/auth/login` require `Authorization: Bearer <accessToken>`.

## Response envelope

Success:
```json
{ "success": true, "data": { ... }, "meta": null, "error": null }
```

Failure:
```json
{ "success": false, "data": null, "meta": null, "error": { "code": "INSUFFICIENT_INVENTORY", "message": "Requested quantity is unavailable" } }
```

Paginated list responses wrap a Spring `Page` object as `data` (its own `content`/`totalElements`/`totalPages`/etc. fields carry pagination metadata — kept there rather than the top-level `meta` field, for consistency across every list endpoint).

## Error codes

| HTTP | Code | Meaning |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Request body failed validation; response `data` holds per-field messages |
| 400 | `BAD_REQUEST` | A business-rule input error (e.g. transfer source = destination) |
| 401 | `UNAUTHORIZED` | Missing/invalid/expired token, or bad credentials |
| 403 | `FORBIDDEN` | Authenticated, but the role lacks the required permission |
| 404 | `NOT_FOUND` | Resource doesn't exist, or belongs to a different organization (never distinguished — see SECURITY.md) |
| 409 | `CONFLICT` / specific codes below | A state conflict |
| 409 | `INSUFFICIENT_INVENTORY` | An inventory operation's conditional UPDATE affected 0 rows — not enough stock |
| 409 | `INVALID_STATUS_TRANSITION` | Order status change isn't a legal transition |
| 409 | `SKU_TAKEN` / `EMAIL_TAKEN` | Uniqueness conflict |
| 409 | `DATA_CONFLICT` | A concurrent request created/modified the same row — safe to retry |
| 429 | `RATE_LIMITED` | Too many requests from this client in the current window |
| 500 | `INTERNAL_ERROR` | Unexpected server error (details logged server-side only) |

## Pagination & sorting

`?page=1&limit=20&sort=field:direction` — 1-indexed pages, `limit` (not `size`), colon (not comma) between sort field and direction. Example: `GET /products?page=1&limit=20&search=laptop&sort=createdAt:desc`.

## Auth — `/auth`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/auth/register` | none | Creates a new organization + its first ADMIN user. Auto-logs in (returns tokens). |
| POST | `/auth/login` | none | `{email, password}` → access token (body) + refresh token (httpOnly cookie) |
| POST | `/auth/refresh` | refresh cookie | Rotates the refresh token, returns a new access token |
| POST | `/auth/logout` | refresh cookie | Revokes the refresh token, clears the cookie |

## Users — `/users`

| Method | Path | Permission | Description |
|---|---|---|---|
| POST | `/users` | `USER_MANAGE` | Create a user in the caller's organization |
| GET | `/users` | `USER_READ` | Paginated list |
| GET | `/users/me` | any authenticated user | Current user's own profile |
| GET | `/users/{id}` | `USER_READ` | |
| PATCH | `/users/{id}/status` | `USER_MANAGE` | Enable/disable a user |

## Products — `/products`

| Method | Path | Permission | Description |
|---|---|---|---|
| POST | `/products` | `PRODUCT_WRITE` | |
| GET | `/products` | `PRODUCT_READ` | Paginated, `?search=&status=` |
| GET | `/products/{id}` | `PRODUCT_READ` | Cached (Redis, 5min TTL) |
| PATCH | `/products/{id}` | `PRODUCT_WRITE` | Partial update; `{"status":"DISABLED"}` disables |

## Warehouses — `/warehouses`

| Method | Path | Permission | Description |
|---|---|---|---|
| POST | `/warehouses` | `WAREHOUSE_WRITE` | |
| GET | `/warehouses` | `WAREHOUSE_READ` | Paginated |
| GET | `/warehouses/{id}` | `WAREHOUSE_READ` | |
| PATCH | `/warehouses/{id}` | `WAREHOUSE_WRITE` | Partial update |

## Customers — `/customers`

| Method | Path | Permission | Description |
|---|---|---|---|
| POST | `/customers` | `CUSTOMER_WRITE` | |
| GET | `/customers` | `CUSTOMER_READ` | Paginated |
| GET | `/customers/{id}` | `CUSTOMER_READ` | |
| PATCH | `/customers/{id}` | `CUSTOMER_WRITE` | Partial update |

## Inventory — `/inventory`

| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/inventory` | `INVENTORY_READ` | Paginated, `?warehouseId=&productId=` |
| GET | `/inventory/{id}/history` | `INVENTORY_READ` | Paginated movement log for one inventory row |
| POST | `/inventory/adjust` | `INVENTORY_WRITE` | `{warehouseId, productId, type: ADD\|REMOVE, quantity, note?}` |
| POST | `/inventory/transfer` | `INVENTORY_WRITE` | `{productId, fromWarehouseId, toWarehouseId, quantity, note?}` |
| POST | `/inventory/reserve` | `INVENTORY_WRITE` | `{warehouseId, productId, quantity}` — the atomic-conditional-UPDATE operation |
| POST | `/inventory/release` | `INVENTORY_WRITE` | Returns reserved stock to available (order cancellation) |
| POST | `/inventory/fulfill` | `INVENTORY_WRITE` | Consumes reserved stock permanently (order completion) |

All five write operations can fail with `409 INSUFFICIENT_INVENTORY` (or `INVALID_RELEASE`/`INVALID_FULFILL` for release/fulfill specifically) — the conditional-UPDATE mechanism described in [ARCHITECTURE.md](ARCHITECTURE.md).

## Orders — `/orders`

| Method | Path | Permission | Description |
|---|---|---|---|
| POST | `/orders` | `ORDER_CREATE` | See body below. Optional `Idempotency-Key` header. |
| GET | `/orders` | `ORDER_READ` | Paginated, `?status=&customerId=` |
| GET | `/orders/{id}` | `ORDER_READ` | |
| PATCH | `/orders/{id}/status` | `ORDER_UPDATE_STATUS` (`ORDER_CANCEL` specifically for `CANCELLED`) | Drives the order state machine |

`POST /orders` body:
```json
{
  "warehouseId": 1,
  "customerId": 1,
  "items": [
    { "productId": 1, "quantity": 2 },
    { "productId": 2, "quantity": 1 }
  ]
}
```

Order status flow: `PENDING → CONFIRMED → PROCESSING → COMPLETED`, with `CANCELLED` reachable from any non-terminal state. `COMPLETED` and `CANCELLED` are terminal. Cancelling releases reserved stock back to available; completing permanently consumes it.

## Audit logs — `/audit-logs`

| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/audit-logs` | `AUDIT_READ` (ADMIN only) | Paginated, `?entity=&action=` |

## Dashboard — `/dashboard`

| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/dashboard` | `REPORT_VIEW` | 6 widgets + recent orders. Cached (Redis, 30s TTL). |

## WebSocket

Connect: `/ws` (SockJS). Subscribe: `/topic/organizations/{organizationId}` for real-time order/inventory notifications (`ORDER_CREATED`, `ORDER_STATUS_CHANGED`, `INVENTORY_LOW`). See [SECURITY.md](SECURITY.md) for the handshake-auth limitation.

## Health

`GET /actuator/health` — liveness/readiness probe groups enabled (`/actuator/health/liveness`, `/actuator/health/readiness`).
