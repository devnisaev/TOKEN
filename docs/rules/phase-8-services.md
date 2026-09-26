# Phase 8 — Production Hardening & Platform Completion

**Status:** Complete (tracks 478–502). Phase 7 completed the event mesh and integration paths; Phase 8 **hardened production flows**, extended CI coverage, and completed deferred consumer/outbox wiring.

Master spec: [PLATFORM-SPEC.md §14](../PLATFORM-SPEC.md#14-phase-8--production-hardening--platform-completion) · Phase 7: [phase-7-services.md](phase-7-services.md)

---

## Goals

1. CI coverage for Phase 7 integration tests (webhooks, credentials, stock split).
2. Notification integration tests for settlement and valuation ops events.
3. Corporate Actions stock-split outbox event for downstream Issuance wiring.
4. Document storage webhook → Registry document verify on `PINNED`.
5. Payment webhook signature verification (provider-configured HMAC).
6. Reporting `valuation.approved` projection; Audit Ledger `settlement.stuck` entries.

**No new microservices** in Phase 8 — extend existing services only.

---

## Build tiers

| Tier | Focus | Track range |
|------|-------|-------------|
| **P1** | CI hardening + Notification Phase 7 tests | 478–487 |
| **P2** | Stock split outbox; document PINNED verify | 488–497 |
| **P3** | Payment webhook HMAC; Reporting/Audit consumers | 498–502 |

---

## Tier 1 — CI & Notification (P1)

| Area | Deliverable |
|------|-------------|
| CI | Phase 7 ITs in `kafka-integration-tests` and hub webhook job |
| Notification | `settlement.stuck`, `settlement.recovered`, `valuation.approved` integration tests |

---

## Tier 2 — Corporate Actions & Document (P2)

| Area | Deliverable |
|------|-------------|
| Corporate Actions | Outbox `stock-split.requested` on `POST /stock-splits` |
| Document | Storage webhook `PINNED` + `documentId` → Registry verify |

---

## Tier 3 — Security & Read Models (P3)

| Area | Deliverable |
|------|-------------|
| Payment | HMAC signature verification on payment webhooks (opt-in per provider) |
| Reporting | `ValuationApprovedRecord` projection from `valuation.approved` |
| Audit Ledger | Immutable entry on `settlement.stuck` |

---

## Testing

| Test | Service |
|------|---------|
| `NotificationKafkaIntegrationTest` (settlement/valuation ops) | notification-service |
| `StockSplitOutboxIntegrationTest` | corporate-actions-service |
| `StorageWebhookIntegrationTest` (verify path) | document-service |
| `PaymentWebhookIntegrationTest` | payment-service |
| `ValuationApprovedReportingIntegrationTest` | reporting-service |
| `AuditKafkaIntegrationTest` (settlement.stuck) | audit-ledger-service |

CI: extend `.github/workflows/ci.yml` `kafka-integration-tests` job.

---

## Related docs

- [kafka-messaging.md](kafka-messaging.md)
- [EVENTS.md](../EVENTS.md)
- [phase-7-services.md](phase-7-services.md)
