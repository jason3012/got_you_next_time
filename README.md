# SettleUp

SettleUp is an expense-sharing application for friend groups. It will connect to a bank account, find transactions that may belong to a group, show each member's balance, and suggest payments to settle the group.

Current status: Phases 1 through 4 are complete. The backend supports groups, shared expenses, balances, and settlement plans.

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

SettleUp uses Plaid Sandbox to retrieve bank transaction data. Create a Plaid Sandbox account and add its client ID and secret to `.env` before using the bank connection features. Each local setup needs its own credentials.

Start PostgreSQL and the backend:

```bash
docker compose up -d
cd backend
./mvnw spring-boot:run
```

The health endpoint is available at <http://localhost:8080/actuator/health>. API documentation is available at <http://localhost:8080/swagger-ui.html>.

## Commands

```bash
npm run phase0:check        # Check local tools
npm run update              # Update tooling and project dependencies
npm run update:tooling      # Update Homebrew-managed tools
npm run update:dependencies # Update backend and frontend dependencies
```

Review dependency changes and run tests before committing an update.
