# ADR-002 — Modular Monolith vs Microservices

**Decision:** A single Spring Boot application, organized into feature packages (`auth/`, `products/`, `inventory/`, `orders/`, ...), not separate deployable services.

**Reason:** The core mandatory requirement — reserving inventory and creating an order without overselling stock — is a single ACID transaction inside one database when everything lives in one process. Split across separate services with separate databases, the exact same operation becomes a distributed transaction (two-phase commit, saga, or similar compensating-transaction machinery), which is a materially harder problem to get right and not something to design correctly for the first time under this deadline. The assignment brief itself explicitly endorses this: "a modular monolith is completely acceptable... do not create microservices merely for the sake of using microservices."

Package boundaries are still real: each feature package only talks to another through its public service interface, never reaches into another package's repository or entity directly, and tenant isolation (`TenantContext`) and RBAC (`@RequiresPermission`) are enforced uniformly at the same layer regardless of which feature is being called. If this needed to split into services later, the boundaries are already drawn — it would be an extraction, not a redesign.

**Trade-off:** Everything scales together (can't scale the orders module independently of the products module), and a bug in one module can in principle affect the availability of the whole process. At this scale, and given the deadline, that trade is clearly worth it — see [ARCHITECTURE.md](ARCHITECTURE.md) for how this would evolve if it ever needed to.
