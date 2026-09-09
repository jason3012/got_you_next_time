#!/usr/bin/env bash
set -euo pipefail

if [ -f package-lock.json ]; then
  npm update
fi

if [ -f frontend/package.json ]; then
  npm --prefix frontend update
fi

if [ -f backend/pom.xml ]; then
  (
    cd backend
    if [ -x ./mvnw ]; then
      ./mvnw versions:use-latest-versions -DgenerateBackupPoms=false
    else
      mvn versions:use-latest-versions -DgenerateBackupPoms=false
    fi
  )
fi

echo "Dependency update complete. Review the diff and run tests before proposing a commit."
