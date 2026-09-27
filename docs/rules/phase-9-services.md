# Phase 9 — Downstream Consumer Completion

**Status:** Complete (tracks 503–527). Phase 8 published stock-split outbox and hardened webhooks; Phase 9 **completes downstream consumers** and read-model projections.

Master spec: [PLATFORM-SPEC.md §15](../PLATFORM-SPEC.md#15-phase-9--downstream-consumer-completion) · Phase 8: [phase-8-services.md](phase-8-services.md)

---

## Goals

1. Token Issuance consumes `stock-split.requested` and publishes `stock-split.completed`.
2. Corporate Actions marks stock-split actions COMPLETED on completion event.
3. Reporting projects `building.approved` and `stock-split.completed`.
4. Integration tests and CI for the corporate-actions → issuance → reporting loop.

**No new microservices** in Phase 9 — extend existing services only.

---

## Build tiers

| Tier | Focus | Track range |
|------|-------|-------------|
| **P1** | Issuance stock-split consumer + completion outbox | 503–512 |
| **P2** | Corporate Actions completion consumer; Reporting projections | 513–522 |
| **P3** | Integration tests, CI, docs | 523–527 |

---

## Tier 1 — Issuance (P1)

| Area | Deliverable |
|------|-------------|
| Token Issuance | `StockSplitRequestedListener` → `StockSplitService.applySplit()` |
| Token Issuance | Outbox `stock-split.completed` after holder/contract ratio update |

---

## Tier 2 — Corporate Actions & Reporting (P2)

| Area | Deliverable |
|------|-------------|
| Corporate Actions | Consume `stock-split.completed` → mark action COMPLETED |
| Reporting | `BuildingApprovedRecord` from `building.approved` |
| Reporting | `StockSplitRecord` from `stock-split.completed` |

---

## Tier 3 — Tests & CI (P3)

| Test | Service |
|------|---------|
| `StockSplitRequestedKafkaIntegrationTest` | token-issuance-service |
| `DividendDistributionRequestedKafkaIntegrationTest` | token-issuance-service |
| `CorporateActionsKafkaIntegrationTest` (stock-split.completed) | corporate-actions-service |
| `BuildingApprovedReportingIntegrationTest` | reporting-service |
| `StockSplitCompletedReportingIntegrationTest` | reporting-service |

CI: extend `.github/workflows/ci.yml` `kafka-integration-tests` job.

---

## Related docs

- [kafka-messaging.md](kafka-messaging.md)
- [EVENTS.md](../EVENTS.md)
- [phase-8-services.md](phase-8-services.md)
