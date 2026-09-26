# Service README API Tables — TokenRealty

Each microservice `README.md` should include an **API overview** table aligned with its Spring `@RestController` mappings.

## Synced (Track 27)

| Service | README | Notes |
|---------|--------|-------|
| Auth | [auth-service/README.md](../../auth-service/README.md) | Auth + user profile endpoints |
| Marketplace | [marketplace-service/README.md](../../marketplace-service/README.md) | Listings, orders, sell, status filter |
| Notification | [notification-service/README.md](../../notification-service/README.md) | Preferences API + Kafka consumer catalog |
| Payment | [payment-service/README.md](../../payment-service/README.md) | Payments, escrow, payouts |
| Property Registry | [token-realty-app/README.md](../../token-realty-app/README.md) | Buildings, flats, SPV, documents |

## When adding endpoints

1. Add row to service README API table (method, path, role, description).
2. If gateway-exposed or BFF, update [api-gateway-bff.md](api-gateway-bff.md) or portal route docs.
3. Kafka events → [EVENTS.md](../EVENTS.md) and optional [schemas/](../schemas/).

## Remaining

Sync API tables for: Token Issuance, Compliance, Rental, Document, Wallet, Blockchain Indexer, API Gateway BFF-only routes.

See [PLATFORM-SPEC.md](../PLATFORM-SPEC.md) §9.6.
