# SettleUp — Build Spec

**What it is:** A friend-group expense app that links to a bank account, automatically flags which transactions belong to a shared group, shows who's over- and underpaying, and computes the fewest transfers needed to settle everyone up.

**Stack:** Java 21 · Spring Boot 3 · PostgreSQL · Plaid · Docker · Kubernetes · React/Vite

---

## How to use this document

**Phase 0 is for you.** It's manual setup — installs, accounts, API keys — that an AI agent can't do. Complete it before starting Phase 1.

**Phases 1+ are written for the AI.** Each phase has a **Goal**, the **Files** it should create or touch, **Acceptance criteria** that define done, and a **Do not yet** list that prevents scope creep into later phases.

**Work one phase per session.** Start a fresh context for each. Paste the phase text plus §2 (the directory map). Do not let the agent skip ahead — the acceptance criteria exist so you have a concrete stopping point, and the "do not yet" lists exist because agents left unconstrained will half-implement four phases at once and leave you with a codebase that doesn't run.

**Rename freely.** Everything below uses `settleup` / `com.settleup`. Pick your own name in Phase 1 and stay consistent.

---

## Phase 0 — Your setup (manual)

### 0.1 Local tooling

| Tool | Version | Check | Notes |
|---|---|---|---|
| **JDK 21** | 21 LTS | `java -version` | Temurin via `brew install --cask temurin@21`. Must be 21 — the spec uses records and modern APIs. |
| **Maven** | 3.9+ | `mvn -v` | `brew install maven`. Or use the wrapper generated in Phase 1. |
| **Docker Desktop** | latest | `docker ps` | Needed for Postgres, image builds, and Testcontainers. Give it ≥4GB RAM in settings. |
| **Node** | 20+ | `node -v` | For the frontend in Phase 12. |
| **kubectl** | latest | `kubectl version --client` | `brew install kubectl` |
| **kind** | latest | `kind version` | `brew install kind`. Local Kubernetes — free, no cloud account needed. |
| **Helm** | 3.x | `helm version` | `brew install helm`. Optional, Phase 11. |

IDE: IntelliJ IDEA Community is free and materially better for Spring than VS Code.

### 0.2 Plaid account

1. Sign up at `dashboard.plaid.com` — no credit card required.
2. Go to **Team Settings → Keys**.
3. Copy your **`client_id`** and your **Sandbox `secret`**.
4. **Stay in Sandbox.** It's free forever, gives realistic fake bank data, and fires all the same webhooks as production. You do not need Production access for this project — do not request it, and do not sign up for a paid plan.
5. Sandbox test credentials for the Link flow: username `user_good`, password `pass_good`.

### 0.3 Webhook tunnel

Plaid needs a public URL to POST webhooks to. Pick one:
- **ngrok** (`brew install ngrok`, free account required for a stable URL), or
- **Cloudflare Tunnel** (`brew install cloudflared`, free, no account for quick tunnels)

You'll run this in Phase 7 and paste the generated HTTPS URL into your env file.

### 0.4 Secrets file

Create `.env` in the repo root once Phase 1 scaffolds it. **Never commit this** — Phase 1 adds it to `.gitignore`.

```bash
POSTGRES_USER=settleup
POSTGRES_PASSWORD=localdevpassword
POSTGRES_DB=settleup

PLAID_CLIENT_ID=<from dashboard>
PLAID_SECRET=<sandbox secret>
PLAID_ENV=sandbox
PLAID_WEBHOOK_URL=<ngrok url, filled in Phase 7>

JWT_SECRET=<generate: openssl rand -base64 48>
TOKEN_ENCRYPTION_KEY=<generate: openssl rand -base64 32>
```

### 0.5 Decisions to lock now

- **App name and package** — used everywhere from Phase 1 on.
- **Money is stored as `long` cents. Never `double`, never `float`.** This is non-negotiable and appears in several phases. Floating-point money bugs are the single most common way a finance project falls apart under scrutiny.
- **Currency:** single-currency (USD) for v1. Multi-currency is a real feature but it doubles the settle-up complexity and adds nothing to the resume story.

---

## Directory map

Target structure. Phases reference these paths directly.

```
settleup/
├── README.md
├── .env                          # gitignored
├── .env.example                  # committed template
├── .gitignore
├── docker-compose.yml            # local Postgres (+ app in Phase 10)
│
├── backend/
│   ├── pom.xml
│   ├── Dockerfile                # Phase 10
│   ├── mvnw / mvnw.cmd
│   └── src/
│       ├── main/
│       │   ├── java/com/settleup/
│       │   │   ├── SettleUpApplication.java
│       │   │   │
│       │   │   ├── common/
│       │   │   │   ├── Money.java                  # long-cents value type
│       │   │   │   ├── ApiError.java
│       │   │   │   ├── GlobalExceptionHandler.java
│       │   │   │   └── exception/
│       │   │   │       ├── NotFoundException.java
│       │   │   │       ├── ForbiddenException.java
│       │   │   │       └── ValidationException.java
│       │   │   │
│       │   │   ├── config/
│       │   │   │   ├── SecurityConfig.java         # Phase 5
│       │   │   │   ├── JacksonConfig.java
│       │   │   │   ├── OpenApiConfig.java
│       │   │   │   └── PlaidConfig.java            # Phase 6
│       │   │   │
│       │   │   ├── user/
│       │   │   │   ├── User.java
│       │   │   │   ├── UserRepository.java
│       │   │   │   ├── UserService.java
│       │   │   │   ├── UserController.java
│       │   │   │   └── dto/
│       │   │   │
│       │   │   ├── group/
│       │   │   │   ├── Group.java
│       │   │   │   ├── GroupMember.java
│       │   │   │   ├── GroupRepository.java
│       │   │   │   ├── GroupMemberRepository.java
│       │   │   │   ├── GroupService.java
│       │   │   │   ├── GroupController.java
│       │   │   │   └── dto/
│       │   │   │
│       │   │   ├── expense/
│       │   │   │   ├── Expense.java
│       │   │   │   ├── ExpenseSplit.java
│       │   │   │   ├── SplitStrategy.java          # EQUAL | EXACT | PERCENTAGE
│       │   │   │   ├── ExpenseRepository.java
│       │   │   │   ├── ExpenseService.java
│       │   │   │   ├── SplitCalculator.java        # remainder-safe division
│       │   │   │   ├── ExpenseController.java
│       │   │   │   └── dto/
│       │   │   │
│       │   │   ├── settlement/
│       │   │   │   ├── BalanceCalculator.java      # Phase 4
│       │   │   │   ├── SettlementPlanner.java      # Phase 4 — the algorithm
│       │   │   │   ├── Settlement.java
│       │   │   │   ├── SettlementRepository.java
│       │   │   │   ├── SettlementService.java
│       │   │   │   ├── SettlementController.java
│       │   │   │   └── dto/
│       │   │   │
│       │   │   ├── auth/                           # Phase 5
│       │   │   │   ├── AuthController.java
│       │   │   │   ├── AuthService.java
│       │   │   │   ├── JwtService.java
│       │   │   │   ├── JwtAuthFilter.java
│       │   │   │   ├── RefreshToken.java
│       │   │   │   ├── RefreshTokenRepository.java
│       │   │   │   └── dto/
│       │   │   │
│       │   │   ├── plaid/                          # Phases 6–7
│       │   │   │   ├── PlaidClient.java
│       │   │   │   ├── PlaidLinkController.java
│       │   │   │   ├── PlaidWebhookController.java
│       │   │   │   ├── PlaidItem.java              # encrypted access token
│       │   │   │   ├── PlaidItemRepository.java
│       │   │   │   ├── TokenEncryptionService.java
│       │   │   │   └── dto/
│       │   │   │
│       │   │   ├── banktxn/                        # Phase 7
│       │   │   │   ├── BankTransaction.java
│       │   │   │   ├── BankTransactionRepository.java
│       │   │   │   ├── TransactionSyncService.java
│       │   │   │   └── dto/
│       │   │   │
│       │   │   └── classification/                 # Phase 8
│       │   │       ├── ExpenseSuggestion.java
│       │   │       ├── SuggestionRepository.java
│       │   │       ├── ClassificationEngine.java
│       │   │       ├── rules/
│       │   │       │   ├── ClassificationRule.java
│       │   │       │   ├── MerchantHistoryRule.java
│       │   │       │   ├── CategoryRule.java
│       │   │       │   └── AmountThresholdRule.java
│       │   │       ├── SuggestionController.java
│       │   │       └── dto/
│       │   │
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-local.yml
│       │       ├── application-docker.yml
│       │       └── db/migration/
│       │           ├── V1__users_and_groups.sql
│       │           ├── V2__expenses_and_splits.sql
│       │           ├── V3__auth_tokens.sql
│       │           ├── V4__plaid_items.sql
│       │           ├── V5__bank_transactions.sql
│       │           └── V6__expense_suggestions.sql
│       │
│       └── test/java/com/settleup/
│           ├── expense/SplitCalculatorTest.java
│           ├── settlement/SettlementPlannerTest.java
│           ├── settlement/BalanceCalculatorTest.java
│           ├── group/GroupAuthorizationTest.java
│           ├── plaid/WebhookIdempotencyTest.java
│           ├── classification/ClassificationEngineTest.java
│           └── support/
│               ├── AbstractIntegrationTest.java    # Testcontainers base
│               └── TestDataFactory.java
│
├── frontend/                     # Phase 12
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
│       ├── main.tsx
│       ├── App.tsx
│       ├── api/client.ts
│       ├── auth/
│       ├── components/
│       ├── pages/
│       │   ├── Login.tsx
│       │   ├── Groups.tsx
│       │   ├── GroupDetail.tsx
│       │   ├── AddExpense.tsx
│       │   ├── SettleUp.tsx
│       │   ├── LinkBank.tsx
│       │   └── SuggestionInbox.tsx
│       └── types/
│
├── k8s/                          # Phase 11
│   ├── base/
│   │   ├── kustomization.yaml
│   │   ├── namespace.yaml
│   │   ├── api-deployment.yaml
│   │   ├── api-service.yaml
│   │   ├── configmap.yaml
│   │   ├── secret.example.yaml
│   │   ├── postgres-statefulset.yaml
│   │   ├── postgres-service.yaml
│   │   ├── hpa.yaml
│   │   └── ingress.yaml
│   └── overlays/
│       ├── local/kustomization.yaml
│       └── eks/kustomization.yaml
│
├── infra/terraform/              # Phase 14 (optional)
│   ├── main.tf
│   ├── vpc.tf
│   ├── eks.tf
│   ├── ecr.tf
│   ├── variables.tf
│   └── outputs.tf
│
├── .github/workflows/            # Phase 13
│   ├── ci.yml
│   └── deploy.yml
│
└── docs/
    ├── architecture.md
    ├── settlement-algorithm.md
    └── api.md
```

---

## Phase 1 — Scaffold and run

**Goal:** An empty but running Spring Boot app connected to Postgres, with Flyway wired and a health endpoint responding.

**Files:** `backend/pom.xml`, `SettleUpApplication.java`, `application.yml`, `application-local.yml`, `docker-compose.yml`, `.gitignore`, `.env.example`, `backend/mvnw`

**Do:**
- Spring Boot 3.x, Java 21. Dependencies: `web`, `data-jpa`, `validation`, `actuator`, `flyway-core`, `postgresql`, `lombok` (optional), `springdoc-openapi-starter-webmvc-ui`.
- `docker-compose.yml` with a `postgres:16` service reading credentials from `.env`, with a named volume and a healthcheck.
- `application.yml`: datasource from env vars, `spring.jpa.hibernate.ddl-auto: validate` (**never `update` — Flyway owns the schema**), Flyway enabled, Actuator exposing `health` and `info`.
- `.gitignore` covering `.env`, `target/`, `node_modules/`, `.idea/`, `*.iml`.
- One placeholder migration `V0__init.sql` (can be a comment) so Flyway has a baseline.

**Acceptance:**
- `docker compose up -d` starts Postgres.
- `./mvnw spring-boot:run` starts cleanly with no errors.
- `curl localhost:8080/actuator/health` returns `{"status":"UP"}`.
- `/swagger-ui.html` loads.

**Do not yet:** entities, security, controllers, business logic, Docker for the app.

---

## Phase 2 — Domain model and schema

**Goal:** The full relational model, defined in Flyway migrations with matching JPA entities.

**Files:** `db/migration/V1__users_and_groups.sql`, `V2__expenses_and_splits.sql`, entities under `user/`, `group/`, `expense/`, plus `common/Money.java`

**Do:**
- Write **SQL migrations first**, then entities to match. Not the reverse.
- Tables: `users`, `groups`, `group_members` (join with a role column), `expenses`, `expense_splits`.
- **All monetary columns are `BIGINT` storing cents.** Name them explicitly: `amount_cents`, `share_cents`. Never `NUMERIC`, never `DOUBLE PRECISION`.
- `Money.java`: a value type wrapping `long` cents with `plus`, `minus`, `negate`, `isZero`, `compareTo`, and formatted display. All arithmetic goes through this — no raw cent math scattered across services.
- UUID primary keys.
- `created_at` / `updated_at` on every table, plus `@CreationTimestamp` / `@UpdateTimestamp`.
- Foreign keys with explicit `ON DELETE` behavior. Think about it: deleting a group should cascade to memberships but must not orphan expense history.
- Indexes on every FK and on `expenses(group_id, created_at)`.
- `SplitStrategy` enum: `EQUAL`, `EXACT`, `PERCENTAGE`.
- Repository interfaces for each aggregate, methods only (no `@Query` yet).

**Acceptance:**
- App boots with `ddl-auto: validate` passing — this proves entities and schema agree.
- `\dt` in psql shows every table with correct constraints.
- No `double`, `float`, or `BigDecimal` anywhere in the money path.

**Do not yet:** services, controllers, auth, split math.

---

## Phase 3 — Groups and expenses (core CRUD)

**Goal:** Create groups, add members, record expenses with splits. No auth yet — endpoints take a `userId` param temporarily.

**Files:** `group/GroupService.java`, `GroupController.java`, `expense/ExpenseService.java`, `SplitCalculator.java`, `ExpenseController.java`, `common/GlobalExceptionHandler.java`, `ApiError.java`, DTOs, `test/expense/SplitCalculatorTest.java`

**Do:**
- Endpoints: create group, list my groups, get group detail, add/remove member, create expense, list group expenses, delete expense.
- **`SplitCalculator` is the important class here.** Splitting $10.00 three ways must produce `334, 333, 333` — not three `333`s that lose a cent. Distribute the remainder deterministically (e.g. to the first N participants by stable ordering) and document the rule.
- Validate: `EXACT` splits must sum to the expense total; `PERCENTAGE` splits must sum to exactly 100; every split participant must be a group member; payer must be a group member.
- DTOs for all request and response bodies. **Never expose JPA entities directly from controllers.**
- `GlobalExceptionHandler` returning a consistent `ApiError` shape (`timestamp`, `status`, `code`, `message`, `fieldErrors`).
- Unit tests for `SplitCalculator` covering: even division, remainder distribution, exact splits that don't sum, percentage rounding, single-member group.

**Acceptance:**
- Full `curl` walkthrough works: create group → add 3 members → add 4 expenses using all three split strategies → list expenses with correct per-person shares.
- Splits always sum exactly to the expense total. Test this with amounts that don't divide evenly.
- Invalid input returns 400 with a useful message, not a 500 stack trace.

**Do not yet:** balances, settle-up, auth, Plaid.

---

## Phase 4 — Balances and the settle-up algorithm

**Goal:** The centerpiece. Compute net balances, then the minimum set of transfers to zero everyone out.

**Files:** `settlement/BalanceCalculator.java`, `SettlementPlanner.java`, `Settlement.java`, `SettlementService.java`, `SettlementController.java`, `db/migration/V3__settlements.sql`, `test/settlement/SettlementPlannerTest.java`, `BalanceCalculatorTest.java`, `docs/settlement-algorithm.md`

**Do:**
- `BalanceCalculator`: for each member, `net = (total they paid) − (total they owe)`. Positive = creditor, negative = debtor. **Assert the sum of all net balances is exactly zero** — if it isn't, there's a split bug, and this assertion is how you catch it.
- `SettlementPlanner`: greedy max-debtor / max-creditor matching.
  - Split members into debtors and creditors, drop zero balances.
  - Repeatedly match the largest debtor against the largest creditor, transfer `min(|debt|, credit)`, remove whoever hits zero.
  - Terminates with at most `n−1` transfers.
  - Use priority queues, not repeated sorts.
- Endpoint `GET /groups/{id}/settle-up` returning the transfer list.
- Endpoint to record a settlement as paid, which creates a `Settlement` row that feeds back into future balance calculations.
- **`docs/settlement-algorithm.md`:** explain the approach, why greedy rather than optimal (minimum-transaction settlement is NP-hard in the general case; greedy gives a near-optimal answer in linearithmic time and is what production apps use), and the exact-cents invariant.

**Acceptance:**
- Tests assert **transaction count**, not just correctness: a 4-person group where A paid everything must settle in exactly 3 transfers, not 6.
- Tests cover: already-settled group (empty result), one debtor many creditors, one creditor many debtors, a group where two members net to zero and must be excluded entirely, amounts that don't divide evenly.
- Property test or fuzz loop: for randomly generated groups, transfers always zero out every balance exactly, and count ≤ n−1.
- Balances remain correct after recording a partial settlement.

**Do not yet:** auth, Plaid, frontend.

---

## Phase 5 — Authentication and authorization

**Goal:** Real auth, and the guarantee that you can only touch groups you belong to.

**Files:** `config/SecurityConfig.java`, `auth/*`, `db/migration/V4__auth_tokens.sql`, `test/group/GroupAuthorizationTest.java`

**Do:**
- Spring Security 6 with a stateless JWT filter chain.
- `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout`.
- BCrypt password hashing. Access token ~15min, refresh token ~30 days, refresh tokens persisted and revocable.
- `JwtAuthFilter` populating `SecurityContext`; resolve the current user via `@AuthenticationPrincipal`.
- **Remove all `userId` request params added in Phase 3** — identity now comes from the token. This is a required cleanup, not optional.
- Authorization checks in `GroupService`: membership required to read a group, and only the expense creator or a group admin may delete an expense.
- Actuator health stays public; everything else requires auth.

**Acceptance:**
- Unauthenticated requests to group endpoints return 401.
- **A user who is not a member of a group gets 403 — with a test proving it.** Authorization bugs are the ones that actually matter; make sure this test exists and fails if you remove the check.
- Login → use access token → expire it → refresh → continue. Full cycle works.
- No password or token hash ever appears in a response body or a log line.

**Do not yet:** Plaid, frontend.

---

## Phase 6 — Plaid Link and item storage

**Goal:** A user can connect a sandbox bank account, and the resulting access token is stored encrypted.

**Files:** `config/PlaidConfig.java`, `plaid/PlaidClient.java`, `PlaidLinkController.java`, `PlaidItem.java`, `PlaidItemRepository.java`, `TokenEncryptionService.java`, `db/migration/V5__plaid_items.sql`

**Do:**
- Add the official `plaid-java` client dependency. Configure with `client_id`, `secret`, `sandbox` environment from env vars.
- `POST /plaid/link-token` — creates a Link token for the authenticated user.
- `POST /plaid/exchange` — exchanges the public token for an access token and `item_id`.
- **`TokenEncryptionService`: AES-GCM encryption of the access token before persistence**, key from `TOKEN_ENCRYPTION_KEY`. The database must never contain a plaintext Plaid access token. Say this in the README; it's a real security decision worth pointing at.
- `PlaidItem` links a user to an institution and stores the encrypted token, `item_id`, institution name, and sync cursor (nullable until Phase 7).
- Endpoints to list and unlink connected accounts (unlinking calls `/item/remove` on Plaid's side too).

**Acceptance:**
- Link token endpoint returns a usable token.
- A manual sandbox Link flow (via Plaid's quickstart page or a curl-driven exchange) results in a `plaid_items` row.
- The `access_token` column is ciphertext when inspected in psql.
- Unlinking removes the row and revokes the item upstream.

**Do not yet:** transaction sync, webhooks, classification.

---

## Phase 7 — Transaction sync and webhooks

**Goal:** Bank transactions flow in automatically and idempotently.

**Files:** `banktxn/*`, `plaid/PlaidWebhookController.java`, `db/migration/V6__bank_transactions.sql`, `test/plaid/WebhookIdempotencyTest.java`

**Do:**
- Use Plaid's **`/transactions/sync`** cursor-based endpoint, not the older date-range one. Persist the cursor on `PlaidItem` after each successful sync.
- `BankTransaction` entity: `plaid_transaction_id` (**unique constraint**), user, amount cents, merchant name, category, date, pending flag, account id.
- Handle all three sync result sets: `added`, `modified`, `removed`.
- `POST /plaid/webhook` receiving `SYNC_UPDATES_AVAILABLE` and triggering a sync for that item.
- **Idempotency is the point of this phase.** Plaid retries webhooks. Processing the same event twice must not duplicate transactions — enforce with the unique constraint plus upsert semantics, and prove it with a test that fires the same webhook payload twice.
- Verify the webhook is genuine (Plaid's JWT verification) rather than trusting any POST to the endpoint.
- Webhook handler returns 200 immediately and processes async (`@Async` or a simple queue) so Plaid doesn't time out.
- Endpoint to list the current user's transactions, paginated.

**Acceptance:**
- Firing a sandbox webhook via the Plaid dashboard (or `/sandbox/item/fire_webhook`) results in transactions appearing in the database.
- **Replaying the identical webhook produces zero new rows.** Test asserts this.
- A `removed` transaction is soft-deleted or removed correctly.
- Malformed or unverified webhook payloads are rejected without a 500.

**Do not yet:** classification, suggestions.

---

## Phase 8 — Classification and the suggestion inbox

**Goal:** Incoming transactions get scored against active groups, and likely group expenses surface for user confirmation.

**Files:** `classification/*`, `db/migration/V7__expense_suggestions.sql`, `test/classification/ClassificationEngineTest.java`

**Do:**
- **Rule-based, not ML.** A `ClassificationRule` interface returning a score 0–1 with a reason string, and a `ClassificationEngine` that combines rule outputs into a confidence per candidate group.
- Ship these rules:
  - `MerchantHistoryRule` — this merchant was previously confirmed as an expense for this group. Highest weight; this is what makes the system get better with use.
  - `CategoryRule` — Plaid's category matches the group's typical categories.
  - `AmountThresholdRule` — amount is in the range typical for this group's expenses.
- `ExpenseSuggestion` entity: transaction, candidate group, confidence, reason strings, status (`PENDING` / `CONFIRMED` / `REJECTED`).
- Only create suggestions above a confidence floor. Below it, the transaction stays personal and silent.
- Endpoints: list pending suggestions, confirm (with an optional split-strategy override), reject.
- **Confirming a suggestion creates a real `Expense` with splits** and marks the source transaction as linked. Rejecting records the negative signal so `MerchantHistoryRule` learns from it.
- Never auto-create an expense without confirmation. Silent misattribution is worse than asking.

**Acceptance:**
- A sandbox restaurant transaction surfaces as a pending suggestion with a human-readable reason.
- Confirming produces a correctly-split expense that shows up in group balances and changes the settle-up plan.
- Confirming a merchant once raises confidence for that merchant next time — test this explicitly.
- Rejecting suppresses future suggestions for that pattern.
- Tests cover the confidence floor: a random unrelated transaction produces no suggestion.

**Do not yet:** Docker, Kubernetes, frontend.

---

## Phase 9 — Test hardening

**Goal:** A real integration test suite running against real Postgres.

**Files:** `test/support/AbstractIntegrationTest.java`, `TestDataFactory.java`, integration tests across all modules

**Do:**
- **Testcontainers** with a `postgres:16` container, `@ServiceConnection` for automatic wiring, reused across the suite for speed.
- `AbstractIntegrationTest` base class; `TestDataFactory` for building users, groups, and expenses without boilerplate.
- Integration tests per module: auth flow end-to-end, group authorization, expense CRUD with splits, settle-up over a realistic multi-expense group, webhook idempotency, suggestion confirm/reject.
- Mock Plaid's HTTP calls (WireMock or a stubbed `PlaidClient`) — tests must not hit Plaid's servers.
- Target meaningful coverage of `SplitCalculator`, `SettlementPlanner`, `BalanceCalculator`, and every authorization check. Coverage elsewhere matters less.

**Acceptance:**
- `./mvnw verify` runs the full suite green from a clean checkout with only Docker running.
- No test depends on network access or on another test's leftover data.
- Suite completes in a reasonable time (reuse the container, don't restart per class).

---

## Phase 10 — Containerize

**Goal:** The app runs as a production-shaped container image.

**Files:** `backend/Dockerfile`, `docker-compose.yml` (extended), `application-docker.yml`, `.dockerignore`

**Do:**
- **Multi-stage build:** Maven build stage → `eclipse-temurin:21-jre-alpine` runtime stage. Copy only the built jar into the final layer.
- **Run as a non-root user.** Create one in the Dockerfile.
- Layer the build so dependency resolution caches independently of source changes (copy `pom.xml`, run `dependency:go-offline`, then copy `src/`).
- All config via environment variables — no secrets baked into the image, no `application-local.yml` in the final layer.
- `HEALTHCHECK` hitting `/actuator/health`.
- Extend `docker-compose.yml` so the API and Postgres come up together, API depending on Postgres's healthcheck.

**Acceptance:**
- `docker compose up` brings up the whole system; the app is reachable and the full curl walkthrough passes.
- Final image is meaningfully smaller than a naive single-stage build — note the size in the README.
- `docker inspect` confirms a non-root user.
- No secrets in `docker history`.

---

## Phase 11 — Kubernetes (local, on kind)

**Goal:** Full Kubernetes deployment running locally. Free, and the manifests are identical to what EKS would use.

**Files:** everything under `k8s/base/` and `k8s/overlays/local/`

**Do:**
- `Deployment` for the API: 2 replicas, **resource requests and limits**, **liveness and readiness probes** pointed at `/actuator/health/liveness` and `/actuator/health/readiness` (enable the probe groups in `application.yml`).
- `Service` (ClusterIP), `ConfigMap` for non-secret config, `Secret` for credentials (commit `secret.example.yaml` only, never the real one).
- Postgres as a `StatefulSet` with a `PersistentVolumeClaim` and its own headless `Service`.
- `HorizontalPodAutoscaler` on CPU (requires metrics-server in kind).
- `Ingress` with an nginx ingress controller.
- **Kustomize** base + overlays so the same manifests target local and EKS with different replica counts and image tags.
- Optionally package as a Helm chart — a frequently-requested JD keyword, and cheap once the manifests exist.

**Acceptance:**
- `kind create cluster` → load image → `kubectl apply -k k8s/overlays/local` → app reachable through the ingress.
- `kubectl delete pod <api-pod>` → traffic continues, pod is rescheduled automatically. **Record this; it's your demo moment.**
- Readiness probe genuinely gates traffic — a pod with a failing DB connection is removed from the Service.
- HPA shows metrics and scales under `hey`/`ab` load.

---

## Phase 12 — Frontend

**Goal:** A usable web UI covering every backend capability.

**Files:** everything under `frontend/`

**Do:**
- React + Vite + TypeScript. Tailwind for styling.
- Typed API client with JWT attached, and automatic refresh on 401.
- Pages: Login/Register, Groups list, Group detail (members, expenses, running balances), Add Expense (all three split strategies), **Settle Up** (the transfer list — this is the money screen, make it clear and satisfying), Link Bank (Plaid Link via `react-plaid-link`), Suggestion Inbox (confirm/reject cards).
- Loading and error states everywhere. Empty states that explain what to do next.
- Mobile-responsive — most of this app's realistic use is on a phone.

**Acceptance:**
- Full flow with no curl: register → create group → add members → link sandbox bank → confirm a suggestion → view balances → view settle-up plan.
- Refresh mid-session doesn't log you out.
- Works at 375px width.

---

## Phase 13 — CI/CD and observability

**Goal:** Automated pipeline and real metrics.

**Files:** `.github/workflows/ci.yml`, `deploy.yml`, observability config

**Do:**
- **CI:** on PR — lint, `./mvnw verify` (Testcontainers works on GitHub runners), frontend build. Fail on test failure.
- **CD:** on merge to main — build image, push to registry, deploy.
- Micrometer + `/actuator/prometheus`.
- Prometheus + Grafana in the local cluster. Build **one** dashboard: request rate, p95 latency, error rate, JVM heap, DB pool utilization. Screenshot it for the README.
- Structured JSON logging with a request correlation ID.

**Acceptance:**
- A PR runs the full suite and blocks on failure.
- Grafana shows live traffic while you click through the app.
- Logs are parseable JSON with correlation IDs traceable across a request.

---

## Phase 14 — EKS (optional, time-boxed) 💰

**Read the cost section before starting. Do not leave this running.**

**Files:** `infra/terraform/*`, `k8s/overlays/eks/`

**Do:**
- Terraform: VPC (**public subnets for worker nodes — avoid a NAT Gateway**), EKS cluster, small managed node group, ECR repository.
- Push image to ECR, apply the EKS overlay of your existing manifests.
- Verify the app serves traffic through the load balancer.
- **Capture proof:** screenshots of `kubectl get nodes/pods` against the real cluster, the AWS console, and a short video of the app running on EKS.
- **`terraform destroy`.** Then manually verify in the console that the load balancer, EBS volumes, and Elastic IPs are gone.

**Acceptance:**
- App is reachable on a real AWS-hosted cluster.
- Proof artifacts are saved outside the cluster.
- `terraform destroy` completes and the AWS console shows no orphaned resources.
- Billing dashboard shows the expected small charge and nothing recurring.

---

## Phase 15 — Ship

**Files:** `README.md`, `docs/architecture.md`, `docs/api.md`

**Do:**
- README: what it does, why the auto-detection angle is different from manual-entry apps, architecture diagram (Mermaid), local setup in under 5 commands, tech stack with brief justifications.
- Call out the decisions worth defending: money as integer cents, encrypted Plaid tokens, webhook idempotency, greedy settlement with its complexity tradeoff, human-in-the-loop classification.
- 90-second demo video.
- **Collect real numbers for resume bullets** — test count, coverage on the core algorithms, image size, p95 latency under load, max group size tested. Measure them; do not estimate.

---

## 💰 Cost summary

| Item | Cost |
|---|---|
| Everything through Phase 13 | **$0** — Plaid Sandbox is free forever, kind runs locally, Testcontainers and GitHub Actions are free at this scale |
| Phase 14: EKS control plane | **~$0.10/hour** (~$73/mo if left up). A 2–3 day window ≈ **$5–8 total** |
| Phase 14: worker nodes + load balancer | Included in the estimate above; the ALB alone is ~$16–18/mo if orphaned |
| NAT Gateway | 🚨 **~$33/mo** — avoid entirely by using public subnets for nodes |
| RDS | Not used. Postgres runs in-cluster. RDS is ~$12–15/mo and **no longer free-tier for new AWS accounts** |

**Before Phase 14:** set an AWS Budgets alert at $5 and $20, and put a calendar reminder to verify the teardown. The expensive AWS mistakes are always the resources you forgot were running.
