#!/usr/bin/env bash
# Start TokenRealty demo infrastructure (Postgres + Kafka).
# After services are running with --spring.profiles.active=local, run ./scripts/seed-demo.sh
#
# Full E2E (requires live gateway on :8080):
#   E2E_GATEWAY_URL=http://localhost:8080 cd frontend/e2e && npm run test:full

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT}"

echo "==> Starting PostgreSQL..."
docker compose up -d postgres

echo "==> Starting Kafka (demo profile)..."
docker compose --profile kafka up -d

echo "==> Waiting for Postgres..."
until docker compose exec -T postgres pg_isready -U postgres >/dev/null 2>&1; do
  sleep 1
done

echo "==> Postgres ready. Databases initialized via docker/postgres/init-databases.sql"

cat <<'EOF'

Demo infrastructure is up.

Next steps:
  1. cd token-issuance-service/hardhat && npm run node && npm run deploy:local
  2. Start backend services with --spring.profiles.active=local
     Minimum for E2E smoke: frontends only (preview mode)
     Minimum for E2E full: auth (:8083), registry (:8081), marketplace (:8084), gateway (:8080)
  3. ./scripts/seed-demo.sh
  4. See docs/hardhat-demo.md for tokenize → list → buy flow

Frontends (separate terminals):
  cd frontend/investor-portal && npm install && npm run dev
  cd frontend/admin-dashboard && npm install && npm run dev
  cd frontend/tenant-portal && npm install && npm run dev

Optional OpenTelemetry (api-gateway):
  OTEL_ENABLED=true OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318
EOF
