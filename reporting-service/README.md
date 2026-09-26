# Reporting Service

Kafka-driven read projections and admin analytics APIs for **TokenRealty**.

Port **8093** · Database **`reporting_service`**

## API

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| GET | `/v1/reports/trading-summary` | ADMIN, PROPERTY_MANAGER | Settled trades, matched orders, matched volume |
| GET | `/v1/reports/occupancy` | ADMIN, PROPERTY_MANAGER | Tokenized vs occupied flat counts |
| GET | `/v1/reports/dividends` | ADMIN, PROPERTY_MANAGER | Dividend distribution aggregates |
| GET | `/v1/reports/export?format=json` | ADMIN, PROPERTY_MANAGER | Regulatory export (trades, orders, dividends, rent) |

## Kafka consumers

When `tokenrealty.kafka.enabled=true`:

| Topic | Projection |
|-------|------------|
| `tokenrealty.marketplace.trade.settled.v1` | `trade_settled_records` |
| `tokenrealty.marketplace.order.matched.v1` | `order_matched_records` |
| `tokenrealty.issuance.dividend.distributed.v1` | `dividend_records` |
| `tokenrealty.payment.rent.collected.v1` | `rent_collected_records` |
| `tokenrealty.registry.flat.tokenized.v1` | `flat_tokenized_records` |

## Run

```bash
psql -U postgres -c "CREATE DATABASE reporting_service;"
./token-realty-app/mvnw -f reporting-service/pom.xml spring-boot:run
```

Tests:

```bash
./token-realty-app/mvnw -f reporting-service/pom.xml test
```
