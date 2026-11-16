# SettleUp API

The REST API uses JSON over HTTP. Local base URL: `http://localhost:8080`. Authenticated users can open the interactive documentation at `/swagger-ui.html`; the machine-readable document is at `/v3/api-docs`.

## Authentication

All application endpoints except registration, login, token refresh, logout, and Plaid webhooks require:

```http
Authorization: Bearer <accessToken>
```

Access tokens expire after one hour and are returned in JSON. Refresh tokens expire after 30 days, are stored only as hashed server-side records, and travel in an HttpOnly, SameSite=Strict cookie. `POST /auth/refresh` rotates the cookie and access token; `POST /auth/logout` revokes it.

## Endpoints

| Method | Path | Auth | Purpose |
|---|---|---:|---|
| `POST` | `/auth/register` | No | Create an account and issue a token pair. |
| `POST` | `/auth/login` | No | Authenticate and issue a token pair. |
| `POST` | `/auth/refresh` | No | Rotate a valid refresh token. |
| `POST` | `/auth/logout` | No | Revoke the refresh cookie and end the session. |
| `GET` | `/users/me` | Yes | Return the authenticated user. |
| `POST` | `/groups` | Yes | Create a group; the caller becomes an administrator. |
| `GET` | `/groups` | Yes | List groups visible to the caller. |
| `GET` | `/groups/{groupId}` | Yes | Return one group and its members. |
| `POST` | `/groups/{groupId}/members` | Admin | Add a registered user by email. |
| `DELETE` | `/groups/{groupId}/members/{userId}` | Admin | Remove a member. |
| `POST` | `/groups/{groupId}/expenses` | Member | Create an expense and its splits. |
| `GET` | `/groups/{groupId}/expenses` | Member | List group expenses newest first. |
| `PUT` | `/groups/{groupId}/expenses/{expenseId}` | Creator or admin | Replace an expense and its splits. |
| `DELETE` | `/groups/{groupId}/expenses/{expenseId}` | Creator or admin | Delete an authorized expense. |
| `GET` | `/groups/{groupId}/balances` | Member | Return derived member balances. |
| `GET` | `/groups/{groupId}/settle-up` | Member | Return a deterministic settlement plan. |
| `POST` | `/groups/{groupId}/settlements` | Debtor or admin | Record a completed payment. |
| `DELETE` | `/groups/{groupId}/settlements/{settlementId}` | Debtor or admin | Reverse a recorded payment. |
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

Expense amounts and exact shares are integer cents. Every expense includes all intended participants once. Each group has one ISO 4217 currency code; manual and imported expenses must use that currency.

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
| `http://localhost:8081/actuator/health/liveness` | Process liveness probe. |
| `http://localhost:8081/actuator/health/readiness` | Readiness probe including the database. |
| `http://localhost:8081/actuator/prometheus` | Prometheus scrape endpoint. |

The management server is separated from the public application port and should remain private in deployed environments. Exact request and response schemas are generated from the controller DTOs and available through OpenAPI.
