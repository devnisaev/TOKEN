#!/usr/bin/env bash
# Start TokenRealty demo infrastructure (Postgres + Kafka).
# After services are running with --spring.profiles.active=local, run ./scripts/seed-demo.sh

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

cat <<'EOF'

Demo infrastructure is up.

Next steps:
  1. cd token-issuance-service/hardhat && npm run node && npm run deploy:local
  2. Start backend services with --spring.profiles.active=local
  3. ./scripts/seed-demo.sh
  4. See docs/hardhat-demo.md for tokenize → list → buy flow

Frontends (separate terminals):
  cd frontend/investor-portal && npm run dev
  cd frontend/admin-dashboard && npm run dev
  cd frontend/tenant-portal && npm run dev
EOF
