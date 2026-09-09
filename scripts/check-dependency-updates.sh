#!/usr/bin/env bash
set -u

exit_code=0

if [ -f package-lock.json ]; then
  npm outdated || exit_code=$?
fi

if [ -f frontend/package.json ]; then
  npm --prefix frontend outdated || exit_code=$?
fi

if [ -f backend/pom.xml ]; then
  (
    cd backend
    if [ -x ./mvnw ]; then
      ./mvnw versions:display-dependency-updates
    else
      mvn versions:display-dependency-updates
    fi
  ) || exit_code=$?
fi

exit "$exit_code"
