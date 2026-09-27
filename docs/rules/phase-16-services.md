# Phase 16 — Institutional Exchange Completion

**Status:** Complete (tracks 678–702). Completes deferred Phase 15 items: **index dividend aggregation**, **loan liquidation**, **OTC surveillance**, **index token issuance**, and **cross-chain bridge MVP**.

Master spec: [PLATFORM-SPEC.md §22](../PLATFORM-SPEC.md#22-phase-16--institutional-exchange-completion) · Diagram: [10-phase-13-rwa-exchange.puml](../diagrams/10-phase-13-rwa-exchange.puml)

---

## Goals

1. **Index dividend aggregation** — on `payout.completed`, accrue pro-rata share for index holders.
2. **Loan liquidation** — undercollateralized loans → collateral seized, ledger entries, status `LIQUIDATED`.
3. **OTC / large-trade surveillance** — Reporting `LARGE_TRADE` alerts on order matched ≥ $500k.
4. **Index token issuance** — Issuance registers index wrapper ERC-1400 contract linked to `IndexDefinition`.
5. **Cross-chain bridge MVP** — Integration Hub bridge transfer requests for foreign secondary listing.

---

## Track backlog (678–702)

| Track range | Focus |
|-------------|-------|
| 678–682 | Index dividend accrual on `payout.completed`; Corporate Actions consume |
| 683–687 | Loan liquidation API; Payment ledger; collateral status transitions |
| 688–692 | Reporting large-trade surveillance; admin alert UI |
| 693–697 | Index token issuance in Issuance; link to Corporate Actions index |
| 698–702 | Cross-chain bridge request API; Integration Hub; E2E tests |

---

## UI

| Portal | Route | Purpose |
|--------|-------|---------|
| Admin | `/surveillance` | Large-trade and liquidation alerts |
| Admin | `/bridge` | Cross-chain transfer queue |

---

## Related docs

- [phase-15-services.md](phase-15-services.md)
- [phase-10-services.md](phase-10-services.md) — surveillance, custody
- [phase-14-services.md](phase-14-services.md) — AMM liquidation path
