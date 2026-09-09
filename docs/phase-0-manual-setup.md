# Phase 0 manual setup

The automated tooling setup intentionally cannot perform the following actions because they require your accounts, secrets, or application settings.

## 1. Install and verify local tools

Run the automated install from the repository root:

```bash
npm run setup:tooling
npm run phase0:check
```

Docker Desktop may ask for macOS permissions or require you to launch it once. In Docker Desktop settings, allocate at least 4 GB of memory. If the doctor reports that Docker is unavailable, start Docker Desktop and rerun `npm run phase0:check`.

The JDK 21 and Docker Desktop installers require your macOS administrator password, which an automated session cannot provide. Run the bundle in your own terminal so Homebrew can show that password prompt. If either install still fails, install them from the official apps and rerun the doctor:

```bash
brew install --cask temurin@21 docker-desktop
```

After JDK 21 is installed, select it for the current shell before building the backend:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
```

## 2. Create Plaid Sandbox credentials

1. Create an account at <https://dashboard.plaid.com>.
2. In **Team Settings → Keys**, copy `client_id` and the **Sandbox** secret.
3. Do not request Production access or a paid plan. This project uses the free Sandbox environment.
4. Later, use the Sandbox Link test login `user_good` / `pass_good`.

Copy `.env.example` to `.env` and put these values there. `.env` is ignored by Git and must never be committed.

## 3. Choose a webhook tunnel

Plaid webhooks need a public HTTPS URL during Phase 7. The tooling bundle installs both supported options; choose one:

- ngrok: create a free account, authenticate the CLI with the token from its dashboard, then run `ngrok http 8080`.
- Cloudflare quick tunnel: run `cloudflared tunnel --url http://localhost:8080`. A Cloudflare account is not required for a quick tunnel.

Put the resulting HTTPS URL in `PLAID_WEBHOOK_URL` in `.env` when Phase 7 begins. Do not expose this file, its values, or Plaid secrets in chat, source control, screenshots, or logs.

## 4. GitHub repository access

The canonical remote is <https://github.com/jason3012/payUp.git>. Sign in to GitHub locally and ensure your account has write access before a later approved push. A browser-based sign-in or a personal access token configured with your normal Git credential manager is sufficient.

## Commit and push rules

- The agent drafts every commit message for your review.
- You must explicitly approve the message before the commit is created. This is required for every commit, without exception.
- The agent pushes only commits whose messages you approved.
- The agent must never add itself as a co-author or include any `Co-authored-by:` trailer.
