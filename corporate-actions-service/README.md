# Corporate Actions Service

Dividend orchestration and future corporate events (splits, rights) for **TokenRealty**.

Port **8097** · Database **`corporate_actions_service`**

## API

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| GET | `/v1/corporate-actions/dividends` | ADMIN, PROPERTY_MANAGER | Paginated dividend actions |
| GET | `/v1/corporate-actions/{actionId}` | ADMIN, PROPERTY_MANAGER | Single corporate action |

No public write endpoints — actions are created from Kafka (`rent.collected`).

## Kafka

When `tokenrealty.kafka.enabled=true`:

| Topic | Direction | Effect |
|-------|-----------|--------|
| `tokenrealty.payment.rent.collected.v1` | consume | Create `CorporateAction` (REQUESTED) |
| `tokenrealty.corporateactions.dividend.distribution-requested.v1` | publish (outbox) | Trigger downstream distribution |
| `tokenrealty.issuance.dividend.distributed.v1` | consume | Mark matching flat+period action COMPLETED |

## Entity

`CorporateAction`: `type` (DIVIDEND, STOCK_SPLIT), `flatId`, `contractId`, `period`, `grossAmountUsd`, `status` (REQUESTED/COMPLETED/FAILED), unique `sourceEventId`.

Contract ID is taken from the rent event when present, otherwise resolved via `GET /v1/tokens/by-flat/{flatId}` on Token Issuance.

## Run

```bash
psql -U postgres -c "CREATE DATABASE corporate_actions_service;"
./token-realty-app/mvnw -f corporate-actions-service/pom.xml spring-boot:run
```

Local profile with Kafka:

```bash
SPRING_PROFILES_ACTIVE=local ./token-realty-app/mvnw -f corporate-actions-service/pom.xml spring-boot:run
```

Tests:

```bash
./token-realty-app/mvnw -f corporate-actions-service/pom.xml test
```
