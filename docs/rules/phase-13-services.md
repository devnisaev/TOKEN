# Phase 13 — RWA Order Book Exchange (CLOB)

**Status:** Planned (tracks 603–627). Evolves Marketplace from **listing-take** model to a **central limit order book** for property tokens with NAV-aware guardrails.

Master spec: [PLATFORM-SPEC.md §19](../PLATFORM-SPEC.md#19-phase-13--rwa-order-book-exchange-clob) · Diagram: [10-phase-13-rwa-exchange.puml](../diagrams/10-phase-13-rwa-exchange.puml)

---

## Goals

1. **CLOB** per property token pair (`TOKEN/USDC`, optionally `TOKEN/TOKEN` same jurisdiction).
2. Price-time priority; partial fills; min tick and lot size per `liquidityTier`.
3. **NAV band**: orders outside ±X% of last attested NAV flagged or rejected.
4. Market data API: depth, trades, 24h volume, last price vs NAV.
5. Reuse existing settlement saga (escrow → transfer → trade.settled).

**Evolve Marketplace Service (`:8084`)** — do not spin out a separate exchange service until order volume justifies split.

---

## CLOB vs current MVP

| | Current (MVP) | Phase 13 CLOB |
|---|---------------|---------------|
| Model | Seller listing → buyer takes | Bid/ask book per token |
| Discovery | Listing grid | Depth chart + order entry |
| Price | Listing fixed price | Limit orders; optional market order vs best ask |
| Partial fill | Full listing amount | Yes |
| NAV check | None on secondary | Band vs `valuation.nav.attested` |

Listing-take remains as **fallback** for `TIER_3` assets and thin markets.

---

## Exchange-native compliance

| Rule | Implementation |
|------|----------------|
| KYC before match | Existing Compliance gate |
| Jurisdiction | Compliance service — investor country vs SPV jurisdiction |
| Accredited-only | ERC-1400 partition or listing flag |
| Self-trade / wash | Phase 10 surveillance + book-level cross-check |
| Holding period | ERC-1400 transfer lock `lockUntil` |
| NAV band | Valuation service attested NAV |

---

## API sketch (Marketplace extension)

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/v1/exchange/orders` | Place limit bid/ask |
| `DELETE` | `/v1/exchange/orders/{id}` | Cancel open order |
| `GET` | `/v1/exchange/orders` | User open orders |
| `GET` | `/v1/exchange/book/{contractId}` | Depth (bids/asks) |
| `GET` | `/v1/exchange/trades/{contractId}` | Recent trades |
| `GET` | `/v1/exchange/ticker/{contractId}` | Last, high, low, volume, navDelta |

Matching engine runs in Marketplace; matched pairs enqueue existing `order.matched` → Payment escrow flow.

---

## Kafka events (proposed)

| Topic | Publisher | Purpose |
|-------|-----------|---------|
| `tokenrealty.marketplace.order.placed.v1` | Marketplace | Book update fan-out |
| `tokenrealty.marketplace.order.cancelled.v1` | Marketplace | Book update |
| `tokenrealty.marketplace.trade.executed.v1` | Marketplace | Market data + Reporting |

Existing `order.matched` / `trade.settled` unchanged for settlement.

---

## Track backlog (603–627)

| Track range | Focus |
|-------------|-------|
| 603–607 | Order book schema; place/cancel limit order API |
| 608–612 | Matching engine; partial fills; idempotent match |
| 613–617 | NAV band validation (Valuation client) |
| 618–622 | Market data endpoints; WebSocket or SSE depth stream |
| 623–627 | Integration tests; investor exchange UI; surveillance on wash patterns |

---

## UI (investor portal)

| Route | Purpose |
|-------|---------|
| `/exchange/{contractId}` | Depth chart, order entry, open orders |
| `/portfolio` | Link to exchange per holding |

---

## Related docs

- [phase-12-services.md](phase-12-services.md) — liquidity tiers
- [phase-14-services.md](phase-14-services.md) — AMM pools
- [investment-limits.md](investment-limits.md)
