#!/usr/bin/env bash
# Start TokenRealty demo infrastructure (Postgres + Kafka + Jaeger).
# Delegates to demo-start.sh — use ./scripts/demo-start.sh --seed after services are up.

set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
exec "${ROOT}/scripts/demo-start.sh" "$@"
