#!/usr/bin/env bash
# Run Playwright full E2E tests against a live gateway.
#
# Usage:
#   ./scripts/e2e-run.sh                         # wait for :8080 then test:full
#   ./scripts/e2e-run.sh --no-wait               # skip health wait
#   E2E_GATEWAY_URL=http://localhost:8080 ./scripts/e2e-run.sh

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT}"

WAIT=true
for arg in "$@"; do
  case "${arg}" in
    --no-wait) WAIT=false ;;
  esac
done

export E2E_GATEWAY_URL="${E2E_GATEWAY_URL:-http://localhost:8080}"

if [[ "${WAIT}" == "true" ]]; then
  SERVICES="8080" TIMEOUT="${E2E_WAIT_TIMEOUT:-120}" ./scripts/wait-for-services.sh
fi

cd frontend/e2e
npm ci
npx playwright install --with-deps chromium
npm run test:full
