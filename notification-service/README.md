# Notification Service

Email notifications driven by Kafka events for **TokenRealty**.

Port **8089** · Database **`notification_service`**

## API

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/v1/notifications/send` | ADMIN | Manual notification trigger (bypasses preference gate) |
| GET | `/v1/notifications/preferences/{userId}` | Any authenticated | Get user notification preferences (defaults when unset) |
| PATCH | `/v1/notifications/preferences/{userId}` | Any authenticated | Upsert `emailEnabled`, `tradeAlerts`, `dividendAlerts`, `rentReminders` |

## Kafka consumers

When `tokenrealty.kafka.enabled=true`, listeners ingest events idempotently via `KafkaEventConsumer` and send email when user preferences allow:

| Topic | Event type |
|-------|------------|
| Building approved | `tokenrealty.registry.building.approved.v1` |
| Flat tokenized | `tokenrealty.registry.flat.tokenized.v1` |
| Listing created | `tokenrealty.marketplace.listing.created.v1` |
| Order matched | `tokenrealty.marketplace.order.matched.v1` |
| Payment confirmed | `tokenrealty.payment.payment.confirmed.v1` |
| Transfer completed | `tokenrealty.issuance.transfer.completed.v1` |
| KYC approved / revoked | `tokenrealty.compliance.investor.kyc-approved.v1` / `kyc-revoked.v1` |
| Trade settled | `tokenrealty.marketplace.trade.settled.v1` |
| Dividend distributed | `tokenrealty.issuance.dividend.distributed.v1` |
| Rent collected / due | `tokenrealty.payment.rent.collected.v1` / `tokenrealty.rental.rent.due.v1` |
| Lease expired | `tokenrealty.rental.lease.expired.v1` |
| Document uploaded | `tokenrealty.document.document.uploaded.v1` |

Dev mode logs emails instead of sending SMTP (`tokenrealty.notification.email.mode=log`).

## Run

```bash
psql -U postgres -c "CREATE DATABASE notification_service;"
./mvnw spring-boot:run
```

Swagger: http://localhost:8089/api/swagger-ui.html

## Tests

```bash
./mvnw test
```

See [docs/EVENTS.md](../docs/EVENTS.md) and [docs/rules/spring-java-services.md](../docs/rules/spring-java-services.md).
