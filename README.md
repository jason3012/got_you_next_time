# SettleUp

SettleUp is an expense-sharing application for friend groups. It will connect to a bank account, find transactions that may belong to a group, show each member's balance, and suggest payments to settle the group.

Current status: Phase 0, local setup.

## Tech stack

- Java 21 and Spring Boot 3 for the backend
- PostgreSQL 16 and Flyway for data and database migrations
- Plaid Sandbox for bank account data
- Docker for local services and app containers
- Kubernetes, kind, and Helm for local deployment work
- React, Vite, TypeScript, and Tailwind for the frontend

## Setup

This project uses macOS and Homebrew for local tool installation.

```bash
git clone https://github.com/jason3012/payUp.git settleUp
cd settleUp
npm run setup
npm run phase0:check
```

`npm run setup` installs the tools listed in the `Brewfile`.

Create a local environment file before Phase 1:

```bash
cp .env.example .env
```

SettleUp uses Plaid Sandbox to retrieve bank transaction data. Create a Plaid Sandbox account and add its client ID and secret to `.env` before using the bank connection features. Each local setup needs its own credentials.

PostgreSQL will run in Docker during Phase 1. Docker Compose will create the `settleup` database and user using the values in `.env`.

## Commands

```bash
npm run phase0:check        # Check local tools
npm run update              # Update tooling and project dependencies
npm run update:tooling      # Update Homebrew-managed tools
npm run update:dependencies # Update backend and frontend dependencies
```

Review dependency changes and run tests before committing an update.

## Project plan

[settleup-build-spec.md](settleup-build-spec.md) describes the build phases and acceptance criteria. The next step is Phase 1, which creates the Spring Boot app and Docker Compose setup.
