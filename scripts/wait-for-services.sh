#!/usr/bin/env bash
# Wait until TokenRealty demo services respond on their health endpoints.
#
# Usage:
#   ./scripts/wait-for-services.sh                    # default minimum E2E stack
#   ./scripts/wait-for-services.sh --timeout 120
#   SERVICES="8080 8083 8081" ./scripts/wait-for-services.sh

set -euo pipefail

TIMEOUT="${TIMEOUT:-90}"
INTERVAL=2
DEADLINE=$((SECONDS + TIMEOUT))

if [[ "${1:-}" == "--timeout" ]]; then
  TIMEOUT="${2:-90}"
  DEADLINE=$((SECONDS + TIMEOUT))
  shift 2
fi

# port:health-path (gateway has no /api prefix on actuator)
DEFAULT_SERVICES=(
  "8080:/actuator/platform-health"
  "8083:/api/actuator/health"
  "8081:/api/actuator/health"
  "8084:/api/actuator/health"
  "8085:/api/actuator/health"
)

if [[ -n "${SERVICES:-}" ]]; then
  # space-separated ports only
  TARGETS=()
  for port in ${SERVICES}; do
    if [[ "${port}" == "8080" ]]; then
      TARGETS+=("${port}:/actuator/platform-health")
    else
      TARGETS+=("${port}:/api/actuator/health")
    fi
  done
else
  TARGETS=("${DEFAULT_SERVICES[@]}")
fi

wait_one() {
  local spec="$1"
  local port="${spec%%:*}"
  local path="${spec#*:}"
  local url="http://localhost:${port}${path}"

  while (( SECONDS < DEADLINE )); do
    if curl -sf "${url}" >/dev/null 2>&1; then
      echo "OK  ${url}"
      return 0
    fi
    sleep "${INTERVAL}"
  done
  echo "TIMEOUT waiting for ${url}" >&2
  return 1
}

echo "Waiting up to ${TIMEOUT}s for demo services..."
for spec in "${TARGETS[@]}"; do
  wait_one "${spec}"
done
echo "All services ready."
