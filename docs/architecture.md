# SettleUp architecture

## System overview

```mermaid
flowchart LR
    U[Browser] -->|HTTPS| W[React + Vite web app]
    W -->|JSON + bearer token| A[Spring Boot API]
    W -->|Plaid Link| P[Plaid Sandbox]
    P -->|verified webhook| A
    A -->|JPA + Flyway| D[(PostgreSQL 16)]
    A -->|Prometheus metrics| M[Prometheus]
    M --> G[Grafana dashboard]
    C[GitHub Actions] -->|build + test| R[GHCR]
    R -->|image rollout| K[kind / Kubernetes]
    K --> A
    K --> D
    K --> M
    K --> G
```

The browser owns presentation and short-lived session state. The API is the authority for identity, membership, expenses, balances, settlements, bank connections, and classification decisions. PostgreSQL is the durable system of record. Plaid supplies bank data but never becomes the source of truth for a group expense until a person confirms or imports it.

## Request and data flows

### Authentication

1. Registration or login returns a signed access token and current user while setting a signed refresh token in an HttpOnly, SameSite=Strict cookie.
2. The web app keeps the access token in memory-backed session storage and sends it as `Authorization: Bearer <token>`.
3. On a `401`, the typed API client uses the refresh cookie once, receives a rotated cookie and access token, and retries the original request.
4. Refresh-token hashes and revocation state are persisted so rotation and logout invalidate prior credentials.
5. Spring Security resolves the user ID from the JWT. Protected endpoints never accept a caller-supplied user ID.

### Expense capture

1. A user creates an expense manually or selects a synced bank transaction.
2. The API validates membership, payer identity, amount, and split inputs.
3. `SplitCalculator` converts equal, exact, or percentage inputs into immutable cent-denominated shares.
4. The expense and its shares commit in one database transaction.
5. `BalanceCalculator` derives each member's current position from expenses and recorded settlements.

### Transaction suggestions

1. Plaid Link exchanges a temporary public token for an access token that is encrypted before storage.
2. Cursor-based sync upserts transactions idempotently; signed webhooks can request another sync.
3. Classification rules score merchant history, category history, and typical amount.
4. Only suggestions meeting the confidence floor enter the inbox.
5. A person confirms or rejects every suggestion. Rejections become negative feedback that suppresses repeated bad matches.

### Settlement planning

Balances use positive cents for creditors and negative cents for debtors. The planner keeps two priority queues and greedily matches the largest debtor with the largest creditor. It is deterministic and runs in `O(n log n + t log n)`, where `n` is the number of members and `t` is the number of transfers. This favors predictable, understandable output over solving the NP-hard minimum-transaction variant.

## Deployment shape

The production-shaped local environment uses a multi-stage, non-root API image. Kubernetes runs two API replicas behind nginx ingress, PostgreSQL on persistent storage, readiness/liveness probes, resource limits, and an HPA spanning two to five replicas. Prometheus discovers the API pods, and Grafana provisions the project dashboard automatically.

GitHub Actions runs frontend linting, tests, builds, and the full backend suite before publishing the API image. A successful `main` CI run may deploy that exact commit when a Kubernetes configuration secret is present.

## Security boundaries

- Passwords are BCrypt hashes; plaintext passwords are never stored.
- Access and refresh tokens are signed and type-scoped; refresh tokens are hashed, rotated, and revocable.
- Authentication endpoints are rate-limited per client address.
- Plaid access tokens are encrypted at rest with a separate application key.
- Plaid webhooks require signature verification before processing.
- Membership and administrative permissions are checked inside the service layer.
- Operational endpoints run on a separate management port that is not exposed by the public Kubernetes service.
- The API container runs as UID/GID `10001`, drops Linux capabilities, and disallows privilege escalation.
- Logs are structured JSON and every response carries `X-Correlation-ID` for request tracing.

## Decisions worth defending

| Decision | Why | Trade-off |
|---|---|---|
| Store money as integer cents | Arithmetic stays exact and auditable. | Currency rules beyond two decimal places require a richer money model. |
| Encrypt Plaid tokens separately | A database disclosure alone does not expose bank credentials. | Key rotation must be handled operationally. |
| Make webhook processing idempotent | Plaid retries cannot duplicate imported state. | Persistence needs stable provider identifiers and upsert logic. |
| Use greedy settlement | Output is fast, deterministic, and easy to explain. | It does not guarantee the mathematically fewest transfers. |
| Keep classification human-in-the-loop | Automation reduces entry work without silently creating debt. | Users still review uncertain transactions. |

Phase 14 can later add an EKS overlay and Terraform infrastructure without changing these application boundaries. The local deployment remains the reference implementation until that optional paid phase is authorized.
