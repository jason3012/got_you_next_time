# SettleUp

SettleUp is an expense-sharing application for friend groups. It will connect to a bank account, find transactions that may belong to a group, show each member's balance, and suggest payments to settle the group.

## Project status

Phase 5 is complete. The project currently has a working backend for the full manual expense-sharing flow:

- Phases 0–2: local tooling, Spring Boot and PostgreSQL setup, Flyway migrations, and core domain entities
- Phases 3–4: group membership, shared expenses, equal/exact/percentage splits, balances, settlement plans, and recorded payments
- Phase 5: account registration and login, BCrypt password hashing, signed JWT access tokens, authenticated user identity, and protected APIs

The next milestone is Phase 6: Plaid Sandbox integration for connecting bank accounts and importing transactions. The React frontend and deployment work remain future milestones.

## Tech stack

- Java 21 and Spring Boot 3 for the backend
- PostgreSQL 16 and Flyway for data and database migrations
- Plaid Sandbox for bank account data
- Docker for local services and app containers
- Kubernetes, kind, and Helm for local deployment work
- React, Vite, TypeScript, Fluent UI, and Radix UI for the frontend

## Setup

This project uses macOS and Homebrew for local tool installation.

```bash
git clone https://github.com/jason3012/payUp.git settleUp
cd settleUp
npm run setup
npm run phase0:check
```

`npm run setup` installs the tools listed in the `Brewfile`.

Create a local environment file:

```bash
cp .env.example .env
```

Generate a JWT signing secret and add it to `.env`:

```bash
openssl rand -base64 48
```

Plaid-backed bank connection features begin in Phase 6. Before working on those features, create a Plaid Sandbox account and add its client ID and secret to `.env`. Each local setup needs its own credentials.

Start PostgreSQL and the backend:

```bash
docker compose up -d
cd backend
./mvnw spring-boot:run
```

The health endpoint is available at <http://localhost:8080/actuator/health>. API documentation is available at <http://localhost:8080/swagger-ui.html>.

Register or sign in to receive an access token:

```bash
curl -X POST http://localhost:8080/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"you@example.com","displayName":"Your Name","password":"a-long-password"}'
```

Send the returned token as `Authorization: Bearer <accessToken>` when calling protected endpoints. User identity is taken from the token; protected endpoints do not accept a `userId` query parameter.

The current API supports:

- account registration, login, and the authenticated user profile
- group creation, listing, membership management, and role-based administration
- expense creation and deletion with equal, exact, or percentage splits
- group balances, suggested settlement transfers, and recorded settlements

## Commands

```bash
npm run phase0:check        # Check local tools
npm run update              # Update tooling and project dependencies
npm run update:tooling      # Update Homebrew-managed tools
npm run update:dependencies # Update backend and frontend dependencies
```

Review dependency changes and run tests before committing an update.
