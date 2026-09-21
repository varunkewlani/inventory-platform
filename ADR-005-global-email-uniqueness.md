# ADR-005 — User Email: Globally Unique, Not Per-Organization

**Decision:** `users.email` is unique across the whole system (`UNIQUE(email)`), not scoped per organization as originally sketched during planning (`UNIQUE(organization_id, email)`).

**Reason:** `POST /auth/login` takes only email and password — no organization selector, matching the assignment's suggested endpoint list exactly. If the same email could exist under two different organizations, login would be ambiguous about which account to authenticate against, with no field in the request to disambiguate. Making email globally unique resolves that ambiguity with zero added UI/API surface (no "pick your organization" step, no per-org login URLs).

**Alternative considered:** Require an organization identifier (a slug, subdomain, or explicit org-selector step) alongside email at login — the more common pattern in larger multi-tenant SaaS products, and what would let two different organizations both have a `same.name@example.com` user. Rejected here specifically for scope: it adds a field to the `organizations` table, a lookup step in the login flow, and a decision about how that identifier is presented to users, none of which the assignment's suggested API surface calls for.

**Trade-off:** A person cannot use the same email address for accounts in two different organizations on this platform (they'd need a second email). This is a real limitation for a general-purpose multi-tenant SaaS product; it's a deliberate, documented scope decision for this assignment, made explicitly under the brief's own guidance to "state your assumptions in the README and proceed" where a requirement doesn't fully specify the answer.
