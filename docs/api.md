# SettleUp API

The REST API uses JSON over HTTP. Local base URL: `http://localhost:8080`. Interactive OpenAPI documentation is available at `/swagger-ui.html`, and the machine-readable document is at `/v3/api-docs`.

## Authentication

All endpoints except registration, login, token refresh, Plaid webhooks, health/metrics, and OpenAPI require:

```http
Authorization: Bearer <accessToken>
```

Access tokens expire after one hour. Refresh tokens expire after 30 days and are exchanged through `POST /auth/refresh`. A refresh response rotates both tokens.

## Endpoints

| Method | Path | Auth | Purpose |
|---|---|---:|---|
| `POST` | `/auth/register` | No | Create an account and issue a token pair. |
| `POST` | `/auth/login` | No | Authenticate and issue a token pair. |
| `POST` | `/auth/refresh` | No | Rotate a valid refresh token. |
| `GET` | `/users/me` | Yes | Return the authenticated user. |
| `POST` | `/groups` | Yes | Create a group; the caller becomes an administrator. |
| `GET` | `/groups` | Yes | List groups visible to the caller. |
| `GET` | `/groups/{groupId}` | Yes | Return one group and its members. |
| `POST` | `/groups/{groupId}/members` | Admin | Add a registered user by email. |
| `DELETE` | `/groups/{groupId}/members/{userId}` | Admin | Remove a member. |
| `POST` | `/groups/{groupId}/expenses` | Member | Create an expense and its splits. |
| `GET` | `/groups/{groupId}/expenses` | Member | List group expenses newest first. |
| `DELETE` | `/groups/{groupId}/expenses/{expenseId}` | Member | Delete an authorized expense. |
| `GET` | `/groups/{groupId}/balances` | Member | Return derived member balances. |
| `GET` | `/groups/{groupId}/settle-up` | Member | Return a deterministic settlement plan. |
| `POST` | `/groups/{groupId}/settlements` | Member | Record a completed payment. |
| `POST` | `/bank/link-token` | Yes | Create a Plaid Link token. |
| `POST` | `/bank/connections` | Yes | Exchange a Plaid public token and save accounts. |
| `GET` | `/bank/connections` | Yes | List the caller's bank connections. |
| `POST` | `/bank/connections/{connectionId}/sync` | Owner | Run cursor-based transaction sync. |
| `DELETE` | `/bank/connections/{connectionId}` | Owner | Disconnect a bank item. |
| `GET` | `/bank/transactions` | Yes | List synced transactions. |
| `POST` | `/bank/transactions/{transactionId}/import` | Owner | Convert a transaction into a group expense. |
| `GET` | `/suggestions` | Yes | List pending classified transactions. |
| `POST` | `/suggestions/{suggestionId}/confirm` | Yes | Confirm a suggestion and create its expense. |
| `POST` | `/suggestions/{suggestionId}/reject` | Yes | Reject a suggestion and retain negative feedback. |
| `POST` | `/plaid/webhook` | Signature | Process verified Plaid transaction updates. |

## Split inputs

Expense amounts and exact shares are integer cents. Every expense includes all intended participants once.

- `EQUAL`: `amountCents` and `percentage` are omitted. Remainder cents are distributed deterministically in request order.
- `EXACT`: every split supplies a non-negative `amountCents`; the shares must equal the expense total.
- `PERCENTAGE`: every split supplies an integer `percentage`; percentages must total exactly 100.

Example:

```json
{
  "description": "Dinner",
  "amountCents": 8420,
  "payerId": "00000000-0000-0000-0000-000000000001",
  "splitStrategy": "PERCENTAGE",
  "splits": [
    { "userId": "00000000-0000-0000-0000-000000000001", "percentage": 60 },
    { "userId": "00000000-0000-0000-0000-000000000002", "percentage": 40 }
  ]
}
```

## Errors and tracing

Errors use a stable JSON envelope:

```json
{
  "timestamp": "2026-09-22T17:00:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Percentage splits must total exactly 100",
  "details": {}
}
```

Every response includes `X-Correlation-ID`. Send that header on a request to preserve an existing trace ID; otherwise the API generates one and adds it to the structured log context.

Common statuses are `400` for invalid input, `401` for missing or expired authentication, `403` for authorization failures, `404` for missing resources, and `409` for conflicting state.

## Operations

| Path | Purpose |
|---|---|
| `/actuator/health/liveness` | Process liveness probe. |
| `/actuator/health/readiness` | Readiness probe including the database. |
| `/actuator/prometheus` | Prometheus scrape endpoint. |

The exact request and response schemas remain generated from the controller DTOs and are available through OpenAPI.
