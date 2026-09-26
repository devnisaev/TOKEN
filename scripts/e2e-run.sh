#!/usr/bin/env bash
# Run Playwright full E2E tests against a live gateway.
#
# Usage:
#   ./scripts/e2e-run.sh                         # wait for :8080 then test:full
#   ./scripts/e2e-run.sh --no-wait               # skip health wait
#   ./scripts/e2e-run.sh --compose-subset        # same 16 specs as ci-compose-e2e.sh
#   E2E_GATEWAY_URL=http://localhost:8080 ./scripts/e2e-run.sh

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT}"

WAIT=true
COMPOSE_SUBSET=false
for arg in "$@"; do
  case "${arg}" in
    --no-wait) WAIT=false ;;
    --compose-subset) COMPOSE_SUBSET=true ;;
  esac
done

export E2E_GATEWAY_URL="${E2E_GATEWAY_URL:-http://localhost:8080}"

if [[ "${WAIT}" == "true" ]]; then
  SERVICES="8080" TIMEOUT="${E2E_WAIT_TIMEOUT:-120}" ./scripts/wait-for-services.sh
fi

cd frontend/e2e
npm ci
npx playwright install --with-deps chromium

if [[ "${COMPOSE_SUBSET}" == "true" ]]; then
  npx playwright test \
    tests/full/investor-login-flow.spec.ts \
    tests/full/investor-portfolio.spec.ts \
    tests/full/investor-buy-flow.spec.ts \
    tests/full/investor-dividend-history.spec.ts \
    tests/full/investor-notification-preferences.spec.ts \
    tests/full/investor-secondary-sell-flow.spec.ts \
    tests/full/investor-secondary-buy-settled.spec.ts \
    tests/full/investor-buy-settled.spec.ts \
    tests/full/admin-kyc-flow.spec.ts \
    tests/full/admin-building-detail.spec.ts \
    tests/full/admin-building-approve.spec.ts \
    tests/full/admin-document-review-flow.spec.ts \
    tests/full/admin-tokenize-flat.spec.ts \
    tests/full/tenant-maintenance-flow.spec.ts \
    tests/full/tenant-rent-flow.spec.ts \
    tests/full/admin-maintenance-flow.spec.ts
else
  npm run test:full
fi
