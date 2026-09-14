# SettleUp

SettleUp is an expense-sharing application for friend groups. It will connect to a bank account, find transactions that may belong to a group, show each member's balance, and suggest payments to settle the group.

## Project status

Phase 7 is complete. The project has a working backend and phone-first frontend for bank-connected expense sharing:

- Phases 0–2: local tooling, Spring Boot and PostgreSQL setup, Flyway migrations, and core domain entities
- Phases 3–4: group membership, shared expenses, equal/exact/percentage splits, balances, settlement plans, and recorded payments
- Phase 5: account registration and login, BCrypt password hashing, signed JWT access tokens, authenticated user identity, and protected APIs
- Phase 6: Plaid Link, encrypted bank credentials, account discovery, cursor-based transaction sync, verified webhooks, and transaction-to-expense conversion
- Phase 7: React authentication and protected routing; a Plaid-first transaction feed; transaction-to-group sharing; friend groups, balances, suggested settlements, and manual fallback expenses; plus the conceptual-sketch landing experience and scroll-linked phone-circle story

The remaining roadmap covers transaction classification and its suggestion inbox, test hardening, containerization, local Kubernetes, CI/CD and observability, and final shipping work as defined in `settleup-build-spec.md`.

## Tech stack

- Java 21 and Spring Boot 3 for the backend
- PostgreSQL 16 and Flyway for data and database migrations
- Plaid Sandbox for bank account data
- Docker for local services and app containers
- Kubernetes, kind, and Helm for local deployment work
- React, Vite, TypeScript, Fluent UI, Radix UI, Drawably, and GSAP for the frontend

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

Generate a separate key for encrypting Plaid access tokens:

```bash
openssl rand -base64 32
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
- Plaid Link tokens, bank connections, transaction synchronization, and importing transactions as expenses

Install and run the frontend during development:

```bash
npm install --prefix frontend
npm run frontend:dev
```

The frontend uses <http://localhost:5173> and connects to the API at <http://localhost:8080> by default. Set `VITE_API_URL` when the backend is hosted elsewhere.

## Commands

```bash
npm run phase0:check        # Check local tools
npm run update              # Update tooling and project dependencies
npm run update:tooling      # Update Homebrew-managed tools
npm run update:dependencies # Update backend and frontend dependencies
npm run frontend:dev        # Start the React development server
npm run frontend:build      # Type-check and build the frontend
npm run frontend:lint       # Type-check the frontend
```

Review dependency changes and run tests before committing an update.
