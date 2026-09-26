# Phase 7 — Event Mesh & Production Integrations

**Status:** Complete (tracks 403–477). Phase 6 scaffolded seven services and Tier 4 hardening; Phase 7 **completes the Kafka event mesh**, production integrations, and scale paths.

Master spec: [PLATFORM-SPEC.md §13](../PLATFORM-SPEC.md#13-phase-7--event-mesh--production-integrations) · Phase 6: [phase-6-services.md](phase-6-services.md)

---

## Goals

1. Wire Phase 6 **publishers** to downstream **consumers** (Notification, Reporting, Registry, Audit Ledger).
2. Add missing topics (`valuation.approved`) where spec promised them.
3. Expand Integration Hub (payment webhooks, outbound adapters).
4. Optional scale path: OpenSearch for Search, KMS for Hub credentials.

**No new microservices** in Phase 7 — extend existing services only.

---

## Build tiers

| Tier | Focus | Track range |
|------|-------|-------------|
| **P1** | Settlement + valuation consumer wiring | 403–427 |
| **P2** | Integration Hub payment webhooks; Search Registry enrichment | 428–452 |
| **P3** | OpenSearch backend; KMS credential rotation | 453–477 |

---

## Tier 1 — Event mesh (P1)

### Publishers (already exist from Phase 6 Tier 4)

| Topic | Publisher |
|-------|-----------|
| `tokenrealty.settlement.stuck.v1` | Settlement |
| `tokenrealty.settlement.recovered.v1` | Settlement |
| `tokenrealty.valuation.updated.v1` | Valuation |
| `tokenrealty.valuation.approved.v1` | Valuation (Phase 7) |

### New consumers

| Topic | Consumer | Action |
|-------|----------|--------|
| `settlement.stuck` | Notification | Admin alert email/log |
| `settlement.stuck` | Reporting | `StuckSagaRecord` projection |
| `settlement.recovered` | Notification | Ops recovery notice |
| `settlement.recovered` | Audit Ledger | Immutable audit entry (ORDER subject) |
| `valuation.updated` | Property Registry | Sync `Valuation` row + token price on flat |
| `valuation.approved` | Notification | Appraisal approved notice |
| `valuation.approved` | Audit Ledger | FLAT subject audit entry |

---

## Tier 2 — Integrations (P2) — implemented

| Area | Deliverable |
|------|-------------|
| Integration Hub | `POST /v1/integrations/webhooks/payment/{provider}` → Payment service |
| Search | Registry client enrichment (building name, city on index rows) |
| Document webhooks | Pinata callback processing beyond 204 ack |

---

## Tier 3 — Scale (P3) — implemented

| Area | Deliverable |
|------|-------------|
| Search | OpenSearch backend (`tokenrealty.search.backend=opensearch`, compose profile `opensearch`) |
| Integration Hub | KMS-backed credential rotation (`POST /v1/integrations/credentials/{type}/{provider}/rotate`) |
| Corporate Actions | Stock split (`POST /v1/corporate-actions/stock-splits`) |

---

## Testing

| Test | Service |
|------|---------|
| `ValuationUpdatedKafkaIntegrationTest` | token-realty-app (Registry) |
| `SettlementStuckReportingIntegrationTest` | reporting-service |
| `SettlementNotificationIntegrationTest` | notification-service |
| Extend `AuditKafkaIntegrationTest` | audit-ledger-service |
| `WebhookRelayIntegrationTest` (payment path) | integration-hub-service |
| `StorageWebhookIntegrationTest` | document-service |
| `SearchKafkaIntegrationTest` (Registry enrichment) | search-service |
| `IntegrationCredentialIntegrationTest` | integration-hub-service |
| `StockSplitIntegrationTest` | corporate-actions-service |

CI: `kafka-integration-tests` and `integration-hub-tests` jobs in `.github/workflows/ci.yml`.

---

## Related docs

- [kafka-messaging.md](kafka-messaging.md) — consumer/outbox rules
- [EVENTS.md](../EVENTS.md) — topic catalog
- [phase-6-services.md](phase-6-services.md) — Phase 6 service ports and deferred items
