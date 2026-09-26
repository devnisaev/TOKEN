#!/usr/bin/env bash
# Start TokenRealty demo infrastructure and print the service startup checklist.
#
# Usage:
#   ./scripts/demo-start.sh              # Postgres + Kafka + Jaeger (demo profile)
#   ./scripts/demo-start.sh --infra-only # Postgres + Kafka only
#   ./scripts/demo-start.sh --seed       # Also run seed-demo.sh when gateway is up
#
# Full E2E (after services are running on :8080):
#   E2E_GATEWAY_URL=http://localhost:8080 cd frontend/e2e && npm run test:full

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT}"

INFRA_ONLY=false
RUN_SEED=false
for arg in "$@"; do
  case "${arg}" in
    --infra-only) INFRA_ONLY=true ;;
    --seed) RUN_SEED=true ;;
  esac
done

echo "==> Starting PostgreSQL..."
docker compose up -d postgres

echo "==> Starting Kafka..."
docker compose --profile kafka up -d

if [[ "${INFRA_ONLY}" == "false" ]]; then
  echo "==> Starting Jaeger (OTLP :4318, UI :16686)..."
  docker compose --profile otel up -d
fi

echo "==> Waiting for Postgres..."
until docker compose exec -T postgres pg_isready -U postgres >/dev/null 2>&1; do
  sleep 1
done

cat <<'EOF'

Demo infrastructure is up.

Minimum backend stack for buy-flow E2E (separate terminals, local profile):
  1. cd token-issuance-service/hardhat && npm run node && npm run deploy:local
  2. Start services (order matters for first boot):
       auth (:8083) → registry (:8081) → payment (:8085) → marketplace (:8084)
       → compliance (:8087) → issuance (:8082) → gateway (:8080)
     Each: ../token-realty-app/mvnw spring-boot:run -Dspring-boot.run.profiles=local
  3. ./scripts/seed-demo.sh
  4. Frontends:
       cd frontend/investor-portal && npm run dev    # :5173
       cd frontend/admin-dashboard && npm run dev    # :5174

OpenTelemetry (gateway + any service with OTEL_ENABLED=true):
  OTEL_ENABLED=true OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318
  Jaeger UI: http://localhost:16686

Full Playwright E2E:
  E2E_GATEWAY_URL=http://localhost:8080 cd frontend/e2e && npm run test:full

See docs/hardhat-demo.md for tokenize → list → buy flow.
EOF

if [[ "${RUN_SEED}" == "true" ]]; then
  if curl -sf http://localhost:8080/actuator/platform-health >/dev/null 2>&1; then
    echo "==> Gateway detected — running seed-demo.sh..."
    "${ROOT}/scripts/seed-demo.sh"
  else
    echo "==> Gateway not reachable on :8080 — start services first, then run ./scripts/seed-demo.sh"
  fi
fi
