# Valuation / NAV Service

Appraisal workflow and token NAV snapshots for **TokenRealty**.

Port **8095** · Database **`valuation_service`** · Package `com.tokenrealty.valuation`

## API

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/v1/valuations` | APPRAISER, ADMIN | Submit valuation for review |
| POST | `/v1/valuations/{id}/approve` | ADMIN | Approve pending valuation, create NAV snapshot |
| POST | `/v1/valuations/{id}/reject` | ADMIN | Reject pending valuation |
| GET | `/v1/valuations/building/{buildingId}` | ADMIN, PROPERTY_MANAGER, APPRAISER | List valuation requests for a building |
| GET | `/v1/valuations/flat/{flatId}/nav` | ADMIN, PROPERTY_MANAGER, APPRAISER | Latest approved NAV snapshot for a flat |

Base URL: `http://localhost:8095/api`

NAV per token: `valueUsd / totalTokens` (BigDecimal, scale 8, half-up).

## Kafka publishes

When `tokenrealty.kafka.enabled=true`:

| Topic | Trigger |
|-------|---------|
| `tokenrealty.valuation.updated.v1` | Valuation approved (includes NAV snapshot fields) |

Outbox relay polls `outbox_events` and publishes via `OutboxRelayWorker`.

## Run

```bash
psql -U postgres -c "CREATE DATABASE valuation_service;"
./token-realty-app/mvnw -pl tokenrealty-security,tokenrealty-web,tokenrealty-jpa,tokenrealty-events,tokenrealty-outbox install
./token-realty-app/mvnw -f valuation-service/pom.xml spring-boot:run
```

Tests:

```bash
./token-realty-app/mvnw -f valuation-service/pom.xml test
```

See [docs/PLATFORM-SPEC.md](../docs/PLATFORM-SPEC.md) §10.14 / §12.3.1.
