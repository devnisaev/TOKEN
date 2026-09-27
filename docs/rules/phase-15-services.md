# Phase 15 — Institutional Exchange (OTC, Indices, Lending)

**Status:** Planned (tracks 653–677). **OTC/RFQ desk**, **REIT-style index baskets**, and **token-collateral lending** for mature secondary markets.

Master spec: [PLATFORM-SPEC.md §21](../PLATFORM-SPEC.md#21-phase-15--institutional-exchange-otc-indices-lending) · Diagram: [10-phase-13-rwa-exchange.puml](../diagrams/10-phase-13-rwa-exchange.puml)

---

## Goals

1. **RFQ / OTC desk** — block trades ($500k+) with compliance pre-check and T+0 on-chain settlement.
2. **Property index tokens** — basket of underlying property tokens (e.g. “US commercial operators”).
3. **Corporate Actions** rebalance for index constituents; pro-rata dividend from underlying payouts.
4. **Collateral lending** — stake property tokens, borrow USDC (LTV capped by NAV + liquidity tier).
5. **Market Maker API** — authenticated quotes and bulk order entry for institutions.

Phases 13–14 CLOB/AMM are **prerequisites** for liquid liquidation paths on lending.

---

## OTC / RFQ

| Step | Actor | Action |
|------|-------|--------|
| 1 | Buyer | `POST /v1/exchange/rfq` — contractId, size, side, expiry |
| 2 | Seller / MM | `POST /v1/exchange/rfq/{id}/quote` |
| 3 | Buyer | Accept quote → escrow + compliance + transfer |
| 4 | Reporting | Large trade surveillance record |

Suited for **`TIER_2` / `TIER_3`** assets and institutional block size.

---

## Index baskets

| Concept | Implementation |
|---------|----------------|
| Index definition | Corporate Actions — weighted list of `contractId`s |
| Index token | New ERC-1400 partition or wrapper contract (Issuance) |
| Rebalance | Corporate Actions event → Issuance adjustment |
| Dividend | Aggregate underlying `payout.completed` → index holders |
| Secondary | Index token trades on CLOB (`TIER_1`) |

Example indices: “Stabilized US gyms”, “NNN industrial”, “Residential yield basket”.

---

## Token-collateral lending

| Rule | Value |
|------|-------|
| Max LTV | 50–65% of attested NAV (tier-dependent) |
| Liquidation | CLOB/AMM/OTC — only `TIER_1`/`TIER_2` collateral |
| Interest | Configurable; ledger in Payment (immutable entries) |
| Custody | Fireblocks integration (Phase 10 `CUSTODY` type) |

**New capability area** — may start as Payment module + Marketplace liquidation hooks; full DeFi lending service deferred until volume proves need.

---

## Cross-chain listing (optional)

Integration Hub + Issuance bridge for foreign investors — list Polygon token on secondary chain. Deferred sub-track within 653–677 if timeboxed.

---

## Track backlog (653–677)

| Track range | Focus |
|-------------|-------|
| 653–657 | RFQ request/quote/accept API; OTC settlement saga |
| 658–662 | Index definition in Corporate Actions; index token issuance |
| 663–667 | Index dividend aggregation; Reporting projections |
| 668–672 | Collateral deposit, borrow, LTV check; Payment ledger |
| 673–677 | Liquidation flow; Market Maker API; institutional UI + E2E |

---

## UI

| Portal | Route | Purpose |
|--------|-------|---------|
| Admin | `/otc` | RFQ queue, approve large trades |
| Investor | `/indices` | Browse index tokens; buy/sell |
| Investor | `/lending` | Collateral dashboard, borrow/repay |
| Admin | `/indices/manage` | Define/rebalance baskets |

---

## Related docs

- [phase-10-services.md](phase-10-services.md) — custody, surveillance
- [phase-13-services.md](phase-13-services.md)
- [phase-14-services.md](phase-14-services.md)
- [corporate actions / issuance rules](../PLATFORM-SPEC.md)
