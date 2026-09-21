# ADR-003 — Concurrency Strategy for Inventory Reservation

**Decision:** Atomic conditional `UPDATE` — `UPDATE inventory SET available_quantity = available_quantity - :qty, reserved_quantity = reserved_quantity + :qty WHERE id = :id AND available_quantity >= :qty`, relying on MySQL/InnoDB's row lock, taken automatically for the statement's duration.

**Reason:** This was evaluated against the alternatives the assignment explicitly names:

- **Optimistic locking** (a `version` column checked on write, retry on conflict) — under real contention (100 requests racing for 10 units), the overwhelming majority of attempts would conflict and need a retry loop, adding complexity and latency proportional to contention for no correctness benefit over the option below.
- **Pessimistic locking** (`SELECT ... FOR UPDATE` then a separate `UPDATE`) — works, but is strictly more code than doing the check and the write in one statement, with a read-then-write gap that has to be reasoned about even though it's protected by the row lock.
- **Distributed locking** (a Redis/Zookeeper-based lock) — solves nothing that MySQL's own row lock doesn't already solve for a single-database system, adds a network hop and a new failure mode.
- **Atomic conditional `UPDATE`** (chosen) — check and write happen in the same statement, MySQL takes the row lock for its duration, and there is never a moment where a race could read stale data and act on it: a second concurrent request against the same row physically blocks until the first commits, then re-evaluates the `WHERE` clause against the now-current value. A statement that matches 0 rows is the unambiguous, immediate signal that the requested quantity isn't available. No retry loop, no explicit lock statement, no second query.

**Verification:** The mandatory concurrency test (`ConcurrencyTest`, Phase 7) fires 100 simultaneous reservation requests against 10 units of stock through the real HTTP API, backed by a real MySQL instance (Testcontainers) — not mocked, not simulated — and asserts exactly 10 succeed and 90 fail cleanly with `INSUFFICIENT_INVENTORY`.

**Trade-off:** `version` is still tracked and incremented on every write, kept specifically for documentation/interview purposes (optimistic-locking bonus per the brief) even though the real guarantee comes from the conditional `UPDATE`, not from a version check. This is a single-database strategy — see [ARCHITECTURE.md](ARCHITECTURE.md) for what changes if inventory is ever sharded across multiple database instances.
