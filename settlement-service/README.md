# Settlement Service

Settlement saga tracker for primary and secondary buy flows on **TokenRealty**.

Port **8094** · Database **`settlement_service`**

## API

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| GET | `/v1/settlements/{orderId}` | ADMIN, PROPERTY_MANAGER | Saga status + step timeline |
| POST | `/v1/settlements/{orderId}/retry` | ADMIN | Mark STUCK saga IN_PROGRESS for ops follow-up |

Escrow hold/release remains in Payment Service — this service tracks visibility only.

## Saga steps

```text
ORDER_MATCHED → PAYMENT_CONFIRMED → TRANSFER_COMPLETED → TRADE_SETTLED
```

## Kafka consumers

When `tokenrealty.kafka.enabled=true`:

| Topic | Effect |
|-------|--------|
| `tokenrealty.marketplace.order.matched.v1` | Start saga |
| `tokenrealty.payment.payment.confirmed.v1` | Advance saga |
| `tokenrealty.issuance.transfer.completed.v1` | Advance saga |
| `tokenrealty.marketplace.trade.settled.v1` | Complete saga |

Scheduled job marks sagas **STUCK** when `updatedAt` exceeds `tokenrealty.settlement.stuck-sla-minutes` (default 30).

## Run

```bash
psql -U postgres -c "CREATE DATABASE settlement_service;"
./token-realty-app/mvnw -f settlement-service/pom.xml spring-boot:run
```

Tests:

```bash
./token-realty-app/mvnw -f settlement-service/pom.xml test
```
