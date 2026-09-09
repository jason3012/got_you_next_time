#!/usr/bin/env bash
set -u

status=0

pass() { printf '✓ %s\n' "$1"; }
fail() { printf '✗ %s\n' "$1"; status=1; }

command_exists() {
  command -v "$1" >/dev/null 2>&1
}

check_command() {
  local command_name="$1"
  local label="$2"
  if command_exists "$command_name"; then
    pass "$label: $(command -v "$command_name")"
  else
    fail "$label is missing. Run: npm run setup:tooling"
  fi
}

java_21_home=""
if [ -x /usr/libexec/java_home ]; then
  java_21_home=$(/usr/libexec/java_home -v 21 2>/dev/null || true)
fi

if [ -n "$java_21_home" ] \
  && [ -x "$java_21_home/bin/java" ] \
  && "$java_21_home/bin/java" -version 2>&1 | head -n 1 | grep -Eq '"21([.\"]|$)'; then
  pass "JDK 21: $($java_21_home/bin/java -version 2>&1 | head -n 1)"
else
  fail "JDK 21 is required. Run: npm run setup:tooling, then set JAVA_HOME with: export JAVA_HOME=\$(/usr/libexec/java_home -v 21)"
fi

check_command mvn "Maven"
check_command node "Node.js"
check_command npm "npm"
check_command kubectl "kubectl"
check_command kind "kind"
check_command helm "Helm"

if command_exists docker; then
  if docker info >/dev/null 2>&1; then
    pass "Docker Desktop is running"
  else
    fail "Docker is installed but unavailable. Start Docker Desktop, then rerun this command."
  fi
else
  fail "Docker Desktop is missing. Run: npm run setup:tooling"
fi

if command_exists cloudflared || command_exists ngrok; then
  pass "Webhook tunnel client available"
else
  fail "No webhook tunnel client found. Run: npm run setup:tooling"
fi

exit "$status"
