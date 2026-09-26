#!/usr/bin/env bash
# CI: start full demo stack and run gateway-backed Playwright E2E subset.
#
# Usage: ./scripts/ci-compose-e2e.sh

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT}"

cleanup() {
  "${ROOT}/scripts/demo-services.sh" --stop 2>/dev/null || true
  docker compose --profile kafka down -v 2>/dev/null || true
}
trap cleanup EXIT

echo "==> Starting infrastructure (Postgres, Redis, Kafka, Schema Registry)..."
docker compose up -d postgres redis
docker compose --profile kafka up -d kafka schema-registry

echo "==> Installing shared libraries..."
"${ROOT}/token-realty-app/mvnw" -f pom.xml \
  -pl tokenrealty-security,tokenrealty-web,tokenrealty-jpa,tokenrealty-kafka,tokenrealty-events,tokenrealty-outbox \
  install -q -DskipTests

echo "==> Starting backend services..."
"${ROOT}/scripts/demo-services.sh"
TIMEOUT="${CI_E2E_WAIT_TIMEOUT:-360}" "${ROOT}/scripts/wait-for-services.sh"

echo "==> Seeding demo data..."
"${ROOT}/scripts/seed-demo.sh"

if [[ "${CI_E2E_HARDHAT:-false}" == "true" ]]; then
  echo "==> Starting Hardhat and tokenizing demo flat (CI_E2E_HARDHAT=true)..."
  mkdir -p "${ROOT}/logs/demo-services"
  (
    cd "${ROOT}/token-issuance-service/hardhat"
    npm ci
    npm run node >"${ROOT}/logs/demo-services/hardhat.log" 2>&1 &
    echo "$! hardhat" >> "${ROOT}/logs/demo-services/.pids"
  )
  sleep 15
  "${ROOT}/scripts/seed-tokenize-demo.sh" || echo "WARN: seed-tokenize-demo skipped (Hardhat may still be starting)"
fi

echo "==> Building frontends..."
(cd "${ROOT}/frontend/shared-ui" && npm ci)
for app in investor-portal admin-dashboard tenant-portal; do
  (cd "${ROOT}/frontend/${app}" && npm ci && npm run build)
done

echo "==> Running Playwright gateway-backed E2E subset..."
cd "${ROOT}/frontend/e2e"
npm ci
npx playwright install --with-deps chromium
export E2E_GATEWAY_URL=http://localhost:8080
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

echo "==> Compose E2E subset passed (16 specs; Hardhat-dependent specs skip when unset)."
