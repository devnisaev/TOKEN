# Phase 6 Services — TokenRealty

**Status:** Complete (tracks 278–402). Seven microservices on ports **8093–8099** extend the platform with read-side analytics, settlement visibility, RWA depth, search, and third-party webhook routing.

Master spec: [PLATFORM-SPEC.md §12](../PLATFORM-SPEC.md#12-phase-6--planned-services) · Kafka: [EVENTS.md](../EVENTS.md) · BFF: [api-gateway-bff.md](api-gateway-bff.md)

---

## Build tiers

| Tier | Priority | Services | Commit batch |
|------|----------|----------|--------------|
| 1 | P1 | Reporting (`:8093`), Settlement Saga (`:8094`) | TOKEN-114 |
| 2 | P2 | Valuation/NAV (`:8095`), Audit Ledger (`:8096`), Corporate Actions (`:8097`) | TOKEN-115 |
| 3 | P3 | Search (`:8098`), Integration Hub (`:8099`) | TOKEN-116 |
| 4 | Hardening | Search BFF, settlement outbox, revaluation schedules, document webhooks, outbox ITs | TOKEN-117 |

**Explicit non-goals:** limits/policy service, config service, Payment escrow split, Issuance deploy/transfer core split.

---

## Service catalog

| Service | Port | DB | Package | Pattern |
|---------|------|-----|---------|---------|
| Reporting / Analytics | 8093 | `reporting_service` | `com.tokenrealty.reporting` | Kafka read projections → REST aggregates |
| Settlement / Saga Tracker | 8094 | `settlement_service` | `com.tokenrealty.settlement` | Kafka ingest + saga state + outbox ops events |
| Valuation / NAV | 8095 | `valuation_service` | `com.tokenrealty.valuation` | Appraisal workflow + NAV snapshots + outbox |
| Audit Ledger | 8096 | `audit_ledger_service` | `com.tokenrealty.audit` | Append-only Kafka audit trail |
| Corporate Actions | 8097 | `corporate_actions_service` | `com.tokenrealty.corporateactions` | `rent.collected` → outbox `dividend.distribution-requested` |
| Search | 8098 | `search_service` | `com.tokenrealty.search` | PostgreSQL search index (OpenSearch deferred) |
| Integration Hub | 8099 | `integration_hub_service` | `com.tokenrealty.integration` | Inbound webhook relay + delivery retry |

README index: [service-readmes.md](service-readmes.md).

---

## Kafka — Phase 6 topics

### Publishes (outbox)

| Topic | Publisher | When |
|-------|-----------|------|
| `tokenrealty.valuation.updated.v1` | Valuation | Admin approves valuation → NAV snapshot |
| `tokenrealty.corporateactions.dividend.distribution-requested.v1` | Corporate Actions | After `rent.collected` creates dividend action |
| `tokenrealty.settlement.stuck.v1` | Settlement | Saga exceeds SLA → status STUCK |
| `tokenrealty.settlement.recovered.v1` | Settlement | Admin `POST .../retry` on STUCK saga |

### Consumes

| Service | Topics |
|---------|--------|
| Reporting | `trade.settled`, `dividend.distributed`, `rent.collected`, `order.matched`, `flat.tokenized` |
| Settlement | `order.matched`, `payment.confirmed`, `transfer.completed`, `trade.settled` |
| Audit Ledger | `kyc-approved`, `kyc-revoked`, `trade.settled`, `document.uploaded`, `order.matched` |
| Corporate Actions | `rent.collected`, `dividend.distributed` |
| Search | `listing.created`, `flat.tokenized`, `building.approved`, `valuation.updated` |
| Token Issuance | `dividend.distribution-requested` (when Corporate Actions owns rent path) |

### Rent → dividend path (production)

```text
rent.collected → Corporate Actions → dividend.distribution-requested (outbox)
                                      → Issuance RentDividendService.distributeForFlat()
                                      → dividend.distributed
```

Disable direct Issuance `RentCollectedListener` when Corporate Actions is active:

```yaml
# token-issuance-service application-local.yml (default in local profile)
tokenrealty:
  dividend:
    rent-collected-listener-enabled: false
```

Keep `rent-collected-listener-enabled: true` in **test** profile for `RentCollectedKafkaIntegrationTest`.

---

## Tier 4 hardening (tracks 378–402)

| Area | Deliverable |
|------|-------------|
| **Search BFF** | Gateway `GET /v1/bff/search/listings`, `/buildings` → Search service |
| **Settlement outbox** | `settlement.stuck` / `settlement.recovered` in same TX as saga update |
| **Valuation schedules** | `RevaluationSchedule` + job + `POST/GET /v1/valuations/schedules` |
| **Integration Hub — document** | `POST /v1/integrations/webhooks/storage/{provider}` → Document ack stub |
| **Outbox ITs** | `OutboxRelayIntegrationTest` on Settlement, Valuation, Corporate Actions |

**Still deferred:** OpenSearch backend, Payment webhooks via Hub, KMS credential rotation, `valuation.approved` topic.

---

## Gateway routes (proxy)

| Path prefix | Target |
|-------------|--------|
| `/api/v1/reports` | Reporting `:8093` |
| `/api/v1/settlements` | Settlement `:8094` |
| `/api/v1/valuations` | Valuation `:8095` |
| `/api/v1/audit` | Audit Ledger `:8096` |
| `/api/v1/corporate-actions` | Corporate Actions `:8097` |
| `/api/v1/search` | Search `:8098` |
| `/api/v1/integrations` | Integration Hub `:8099` |

---

## Integration Hub webhooks

| Ingress (Hub) | Downstream | Auth |
|---------------|------------|------|
| `POST /v1/integrations/webhooks/kyc/{provider}` | Compliance `POST /v1/compliance/webhooks/kyc/{provider}` | Signature headers passthrough; `permitAll` |
| `POST /v1/integrations/webhooks/storage/{provider}` | Document `POST /v1/documents/webhooks/{provider}` | Ack stub (204) in v1 |

Delivery tracking: `IntegrationDelivery` entity, retry worker (max 5 attempts, exponential backoff). Admin: `GET /v1/integrations/deliveries`.

---

## Scaffolding checklist (new Phase 6–style service)

Copy from `reporting-service/` (Kafka consumer projections) or `marketplace-service/` (Kafka in + outbox).

- [ ] Port + DB in [PLATFORM-SPEC.md §7](../PLATFORM-SPEC.md) and `docker/postgres/init-databases.sql`
- [ ] Root `pom.xml` module + gateway route + demo scripts + CI matrix
- [ ] `KafkaEventConsumer` + `processed_events` dedupe for every consumer
- [ ] Outbox enqueue in **same TX** as domain write when publishing
- [ ] `application-test.yml` with H2; Kafka IT with `KafkaEventConsumer` direct invoke pattern
- [ ] README API table; link in [docs/README.md](../README.md)

---

## Related docs

- [kafka-messaging.md](kafka-messaging.md) — outbox relay, listener rules
- [api-gateway-bff.md](api-gateway-bff.md) — BFF search and admin aggregates
- [spring-java-services.md](spring-java-services.md) — layering and RestClient conventions
