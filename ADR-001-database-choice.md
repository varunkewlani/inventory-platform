# ADR-001 — Database: MySQL vs PostgreSQL vs MongoDB

**Decision:** MySQL (InnoDB).

**Reason:** User-specified for this project. Beyond that constraint, it's a good fit anyway: orders and inventory need strong transactional consistency and row-level locking under concurrent writes (the mandatory concurrency requirement), which InnoDB provides directly via its default `UPDATE ... WHERE` row-locking behavior — no extra infrastructure needed. A document store (MongoDB) would make the relational structure here (orgs → users/warehouses/products/orders, orders → order_items, multi-table joins for the dashboard) more awkward to model and would require application-level transaction handling for the multi-step order-creation flow instead of a single ACID transaction.

**Trade-off:** Horizontal scaling (write scaling specifically) requires more deliberate planning than a natively-distributed store — read replicas and, if ever needed, sharding by `organization_id` are the standard path, discussed in [ARCHITECTURE.md](ARCHITECTURE.md)'s scalability section. Not a concern at this project's scale.
