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
  tests/full/admin-kyc-flow.spec.ts \
  tests/full/tenant-maintenance-flow.spec.ts \
  tests/full/admin-maintenance-flow.spec.ts

echo "==> Compose E2E subset passed."
