# Phase 10 — Institutional Expansion (Gemini Roadmap Adaptation)

**Status:** Complete (tracks 528–552). Adapts institutional/DeFi ideas from the Gemini expansion roadmap by **extending existing services** and adding **Governance Service** (`:8100`).

Master spec: [PLATFORM-SPEC.md §16](../PLATFORM-SPEC.md#16-phase-10--institutional-expansion) · Phase 9: [phase-9-services.md](phase-9-services.md)

Architecture diagram: [08-phase-10-institutional-expansion.puml](../diagrams/08-phase-10-institutional-expansion.puml)

---

## Goals

1. Integration Hub adapters for valuation feeds, custody (Fireblocks), and fiat ramp (MoonPay).
2. Valuation AVM feed ingest + `nav.attested` outbox on approve.
3. Payment dividend withholding + Reporting tax/surveillance projections.
4. New **Governance Service** (`:8100`) — token-holder proposals and votes.
5. Marketplace self-trade surveillance guard.

**One new microservice:** `governance-service` (`:8100`). All other work extends existing services.

---

## Build tiers

| Tier | Focus | Track range |
|------|-------|-------------|
| **P1** | Hub adapters (valuation feed, custody, ramp) | 528–537 |
| **P2** | Valuation feed ingest + NAV attestation outbox | 538–547 |
| **P3** | Tax withholding, Reporting projections, Governance MVP | 548–552 |

---

## Tier 1 — Integration Hub (P1)

| Area | Deliverable |
|------|-------------|
| Integration Hub | `IntegrationType.VALUATION_FEED`, `CUSTODY`; webhook relay to Valuation |
| Integration Hub | MoonPay/Stripe payment webhook path (extends Phase 8) |
| Integration Hub | Fireblocks credential type on credential API |

---

## Tier 2 — Valuation depth (P2)

| Area | Deliverable |
|------|-------------|
| Valuation | `POST /v1/valuations/feeds/{provider}` ingest from Hub |
| Valuation | Outbox `valuation.nav.attested.v1` on approve |

---

## Tier 3 — Tax, surveillance, governance (P3)

| Area | Deliverable |
|------|-------------|
| Payment | Configurable withholding on dividend payouts |
| Reporting | `TaxSummaryRecord`, `SurveillanceAlertRecord` projections |
| Marketplace | Self-trade rejection on secondary buy |
| Governance | New service `:8100` — proposals, votes, `governance.proposal.closed` outbox |

---

## Frontend & BFF (planned — not in tracks 528–552)

Backend institutional features need portal and gateway work. Full checklist: [PLATFORM-SPEC.md §16.6](../PLATFORM-SPEC.md#166-frontend--bff-planned--ui-not-in-phase-10-scope). Suggested UI tracks **553–577**.

| Portal | Key additions |
|--------|---------------|
| **Investor** (`:5173`) | Dividends withholding breakdown; governance list + vote; self-trade error on secondary buy |
| **Admin** (`:5174`) | Governance CRUD; Hub integrations + webhook log; compliance reports (tax + surveillance); NAV attestation on building detail |
| **Tenant** (`:5175`) | — (no changes) |
| **Gateway BFF** | Governance proxy; investor governance filter; admin compliance aggregate |

**Prerequisites before UI:** Reporting query endpoints for tax/surveillance projections; gateway route to Governance `:8100`; dividend/payout API fields exposed to investors.

---

## Deferred (Phases 11–15)

| Idea from Gemini doc | Adaptation |
|---------------------|------------|
| Institutional UI | [Phase 11](phase-11-services.md) — governance, tax, surveillance portals |
| Universal assets (gyms, land, stations) | [Phase 12](phase-12-services.md) — AssetUnit model |
| RWA order book | [Phase 13](phase-13-services.md) — CLOB in Marketplace |
| AMM liquidity | [Phase 14](phase-14-services.md) — pools + NAV circuit breakers |
| DeFi lending / OTC / indices | [Phase 15](phase-15-services.md) — RFQ, baskets, collateral |
| Cross-chain bridge | Phase 15 sub-track — Integration Hub + Issuance |
| IoT / ESG / Insurance | Future vertical services `:8119+` |

---

## Testing

| Test | Service |
|------|---------|
| `ValuationFeedIntegrationTest` | valuation-service |
| `WebhookRelayIntegrationTest` (valuation feed path) | integration-hub-service |
| `DividendWithholdingIntegrationTest` | payment-service |
| `TaxSummaryReportingIntegrationTest` | reporting-service |
| `SelfTradeSurveillanceIntegrationTest` | marketplace-service |
| `GovernanceIntegrationTest` | governance-service |

CI: extend `kafka-integration-tests` and `integration-hub-tests`.

---

## Related docs

- [kafka-messaging.md](kafka-messaging.md)
- [EVENTS.md](../EVENTS.md)
- [phase-9-services.md](phase-9-services.md)
