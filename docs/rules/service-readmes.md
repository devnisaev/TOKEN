# Service README API Tables — TokenRealty

Each microservice `README.md` should include an **API overview** table aligned with its Spring `@RestController` mappings.

## Synced

| Service | README |
|---------|--------|
| API Gateway | [api-gateway/README.md](../../api-gateway/README.md) |
| Auth | [auth-service/README.md](../../auth-service/README.md) |
| Property Registry | [token-realty-app/README.md](../../token-realty-app/README.md) |
| Token Issuance | [token-issuance-service/README.md](../../token-issuance-service/README.md) |
| Marketplace | [marketplace-service/README.md](../../marketplace-service/README.md) |
| Payment | [payment-service/README.md](../../payment-service/README.md) |
| Rental | [rental-service/README.md](../../rental-service/README.md) |
| Compliance | [compliance-service/README.md](../../compliance-service/README.md) |
| Document | [document-service/README.md](../../document-service/README.md) |
| Notification | [notification-service/README.md](../../notification-service/README.md) |
| Wallet | [wallet-service/README.md](../../wallet-service/README.md) |
| Blockchain Indexer | [blockchain-indexer-service/README.md](../../blockchain-indexer-service/README.md) |
| Reporting / Analytics | [reporting-service/README.md](../../reporting-service/README.md) |
| Settlement / Saga Tracker | [settlement-service/README.md](../../settlement-service/README.md) |
| Valuation / NAV | [valuation-service/README.md](../../valuation-service/README.md) |
| Audit Ledger | [audit-ledger-service/README.md](../../audit-ledger-service/README.md) |
| Corporate Actions | [corporate-actions-service/README.md](../../corporate-actions-service/README.md) |
| Search | [search-service/README.md](../../search-service/README.md) |
| Integration Hub | [integration-hub-service/README.md](../../integration-hub-service/README.md) |

Phase 6 overview: [phase-6-services.md](phase-6-services.md).

## When adding endpoints

1. Add row to service README API table (method, path, role, description).
2. If gateway-exposed or BFF, update [api-gateway-bff.md](api-gateway-bff.md) or portal route docs.
3. Kafka events → [EVENTS.md](../EVENTS.md) and optional [schemas/](../schemas/).

## Maintenance

Re-sync when controllers change. BFF-only routes are documented in API Gateway README and [api-gateway-bff.md](api-gateway-bff.md), not duplicated in downstream service READMEs.

See [PLATFORM-SPEC.md](../PLATFORM-SPEC.md) §9.6.
