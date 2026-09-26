#!/usr/bin/env bash
# One-command TokenRealty demo: infra → services → wait → seed [→ E2E].
#
# Usage:
#   ./scripts/demo-all.sh              # full demo stack (no E2E)
#   ./scripts/demo-all.sh --e2e        # also run Playwright full tests
#   ./scripts/demo-all.sh --tokenize   # also run seed-tokenize-demo.sh (Hardhat)
#   ./scripts/demo-all.sh --tokenize --buy  # tokenize then demo-buy-flow.sh
#   ./scripts/demo-all.sh --maintenance     # print tenant maintenance portal hint after seed
#   ./scripts/demo-all.sh --infra-only # Postgres + Kafka only (via demo-start)
#   ./scripts/demo-all.sh --stop       # stop background Spring Boot processes

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT}"

RUN_E2E=false
RUN_TOKENIZE=false
RUN_BUY=false
RUN_MAINTENANCE=false
INFRA_ONLY=false
STOP=false

for arg in "$@"; do
  case "${arg}" in
    --e2e) RUN_E2E=true ;;
    --tokenize) RUN_TOKENIZE=true ;;
    --buy) RUN_BUY=true ;;
    --maintenance) RUN_MAINTENANCE=true ;;
    --infra-only) INFRA_ONLY=true ;;
    --stop) STOP=true ;;
    *)
      echo "Unknown option: ${arg}" >&2
      echo "Usage: $0 [--e2e] [--tokenize] [--buy] [--maintenance] [--infra-only] [--stop]" >&2
      exit 1
      ;;
  esac
done

if [[ "${RUN_BUY}" == "true" && "${RUN_TOKENIZE}" != "true" ]]; then
  echo "--buy requires --tokenize (demo buy needs a tokenized flat + listing)" >&2
  exit 1
fi

if [[ "${STOP}" == "true" ]]; then
  exec "${ROOT}/scripts/demo-services.sh" --stop
fi

if [[ "${INFRA_ONLY}" == "true" ]]; then
  exec "${ROOT}/scripts/demo-start.sh" --infra-only
fi

echo "==> Step 1/4: Starting demo infrastructure..."
"${ROOT}/scripts/demo-start.sh" --infra-only

echo ""
echo "==> Step 2/4: Starting backend services..."
"${ROOT}/scripts/demo-services.sh"

echo ""
echo "==> Step 3/4: Waiting for gateway health..."
SERVICES="8080" TIMEOUT="${DEMO_WAIT_TIMEOUT:-180}" "${ROOT}/scripts/wait-for-services.sh"

echo ""
echo "==> Step 4/4: Seeding demo data..."
"${ROOT}/scripts/seed-demo.sh"

if [[ "${RUN_TOKENIZE}" == "true" ]]; then
  echo ""
  echo "==> Optional: tokenize demo flat + wait for listing..."
  "${ROOT}/scripts/seed-tokenize-demo.sh"

  if [[ "${RUN_BUY}" == "true" ]]; then
    echo ""
    echo "==> Optional: place demo buy order..."
    "${ROOT}/scripts/demo-buy-flow.sh"
  fi
fi

cat <<'EOF'

Demo stack is ready.

Hardhat (required for buy / on-chain flows):
  cd token-issuance-service/hardhat && npm run node && npm run deploy:local

Frontends:
  cd frontend/investor-portal && npm run dev    # :5173
  cd frontend/admin-dashboard && npm run dev    # :5174
  cd frontend/tenant-portal && npm run dev      # :5175

Tenant maintenance tickets:
  http://localhost:5175/maintenance  (tenant@demo.com / tenant123)

Admin maintenance queue:
  http://localhost:5174/maintenance

GraphQL BFF (local profile):
  http://localhost:8080/graphiql

Full Playwright E2E:
  ./scripts/e2e-run.sh --no-wait
  ./scripts/e2e-run.sh --compose-subset --no-wait        # CI compose subset (16 specs)
  ./scripts/e2e-run.sh --compose-subset --hardhat --no-wait  # + Hardhat tokenize seed
EOF

if [[ "${RUN_MAINTENANCE}" == "true" ]]; then
  echo ""
  echo "==> Maintenance demo: tenant portal /maintenance, admin /maintenance"
fi

if [[ "${RUN_E2E}" == "true" ]]; then
  echo ""
  echo "==> Running Playwright full E2E..."
  "${ROOT}/scripts/e2e-run.sh" --no-wait
fi
