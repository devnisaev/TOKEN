#!/usr/bin/env bash
# Build a Spring Boot service JAR and Docker image.
#
# Usage: ./scripts/docker-build.sh api-gateway 8080

set -euo pipefail

SERVICE="${1:?service folder name (e.g. api-gateway)}"
PORT="${2:-8080}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT}"

JAR="${SERVICE}-0.0.1-SNAPSHOT.jar"

echo "==> Installing shared libraries..."
./token-realty-app/mvnw -f pom.xml -pl tokenrealty-security,tokenrealty-web,tokenrealty-jpa,tokenrealty-kafka,tokenrealty-events,tokenrealty-outbox install -q

echo "==> Building ${SERVICE}..."
./token-realty-app/mvnw -f "${SERVICE}/pom.xml" package -DskipTests -q

echo "==> Building Docker image tokenrealty/${SERVICE}:local..."
docker build -f docker/Dockerfile.spring-service \
  --build-arg "SERVICE_DIR=${SERVICE}" \
  --build-arg "SERVICE_JAR=${JAR}" \
  --build-arg "EXPOSE_PORT=${PORT}" \
  -t "tokenrealty/${SERVICE}:local" .

echo "Built tokenrealty/${SERVICE}:local (port ${PORT})"
