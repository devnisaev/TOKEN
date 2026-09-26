#!/usr/bin/env bash
# Start the minimum TokenRealty buy-flow backend stack in background.
#
# Usage:
#   ./scripts/demo-services.sh              # start auth → … → document → wallet → indexer → gateway
#   ./scripts/demo-services.sh --stop       # stop background Spring Boot processes started by this script
#
# Prerequisites:
#   ./scripts/demo-start.sh                 # Postgres (+ optional Kafka)
#   cd token-issuance-service/hardhat && npm run node && npm run deploy:local   # for on-chain flows
#
# Logs: logs/demo-services/<service>.log

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT}"

PID_FILE="${ROOT}/logs/demo-services/.pids"
MVNW="${ROOT}/token-realty-app/mvnw"
PROFILE="${SPRING_PROFILES:-local}"
LOG_DIR="${ROOT}/logs/demo-services"

stop_services() {
  if [[ ! -f "${PID_FILE}" ]]; then
    echo "No demo-services PID file at ${PID_FILE}"
    return 0
  fi
  while read -r pid name; do
    if kill -0 "${pid}" 2>/dev/null; then
      echo "Stopping ${name} (pid ${pid})..."
      kill "${pid}" 2>/dev/null || true
    fi
  done < "${PID_FILE}"
  rm -f "${PID_FILE}"
  echo "Demo services stopped."
}

if [[ "${1:-}" == "--stop" ]]; then
  stop_services
  exit 0
fi

mkdir -p "${LOG_DIR}"
: > "${PID_FILE}"

start_one() {
  local name="$1"
  local dir="$2"
  local log="${LOG_DIR}/${name}.log"

  echo "Starting ${name} (profile=${PROFILE}) → ${log}"
  (
    cd "${ROOT}/${dir}"
    exec "${MVNW}" spring-boot:run -Dspring-boot.run.profiles="${PROFILE}"
  ) >"${log}" 2>&1 &
  echo "$! ${name}" >> "${PID_FILE}"
}

echo "==> Ensuring Postgres is up..."
docker compose up -d postgres >/dev/null 2>&1 || true

start_one "auth" "auth-service"
sleep 3
start_one "registry" "token-realty-app"
start_one "payment" "payment-service"
start_one "marketplace" "marketplace-service"
start_one "compliance" "compliance-service"
start_one "issuance" "token-issuance-service"
start_one "rental" "rental-service"
start_one "notification" "notification-service"
start_one "document" "document-service"
start_one "wallet" "wallet-service"
start_one "indexer" "blockchain-indexer-service"
start_one "reporting" "reporting-service"
start_one "settlement" "settlement-service"
start_one "valuation" "valuation-service"
start_one "audit-ledger" "audit-ledger-service"
start_one "corporate-actions" "corporate-actions-service"
sleep 2
start_one "gateway" "api-gateway"

echo ""
echo "Services starting in background. Wait for health checks:"
echo "  ${ROOT}/scripts/wait-for-services.sh"
echo ""
echo "Then seed demo data:"
echo "  ${ROOT}/scripts/seed-demo.sh"
echo ""
echo "Stop all:"
echo "  ${ROOT}/scripts/demo-services.sh --stop"
echo ""
echo "Tail logs:"
echo "  tail -f ${LOG_DIR}/gateway.log"
