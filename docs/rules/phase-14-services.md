# Phase 14 — AMM Liquidity Pools

**Status:** Complete (tracks 628–652). **Automated market maker** pools for fractional property tokens (`PropertyToken ↔ USDC`) with **NAV circuit breakers**.

Master spec: [PLATFORM-SPEC.md §20](../PLATFORM-SPEC.md#20-phase-14--amm-liquidity-pools) · Diagram: [10-phase-13-rwa-exchange.puml](../diagrams/10-phase-13-rwa-exchange.puml)

---

## Goals

1. Constant-product or stable-swap pools per **`TIER_1`** property token.
2. LP tokens for market makers; swap fees (protocol + optional property SPV share).
3. **NAV circuit breaker**: pause swaps when pool price diverges > Y% from attested NAV.
4. Integration with Payment escrow for swap settlement (same invariant: KYC before token movement).
5. Flash-loan protection hooks (deferred from Phase 10 Gemini roadmap).

**Extend Marketplace + Payment** — pool state in Marketplace DB; swaps trigger escrow + Issuance transfer path.

---

## Why AMM for real estate tokens

Fractional RE is **illiquid**. CLOB alone may have empty books. AMM provides:

- Always-on quote for small tickets
- LP incentives for institutional market makers
- Price discovery bounded by NAV oracle (not pure DeFi speculation)

**Not suitable for:** raw land (`TIER_3`), pre-revenue development — CLOB/RFQ only.

---

## Pool lifecycle

```text
1. Admin or governance approves pool for contractId (TIER_1, NAV attested)
2. Seed liquidity: LP deposits TOKEN + USDC
3. Swaps: investor swaps USDC → TOKEN (KYC + compliance)
4. NAV attestation (Valuation) → circuit breaker check
5. If |poolPrice - navPerToken| > threshold → pause pool, alert Reporting
6. LP withdraw (subject to lock period)
```

---

## Components

| Component | Owner | Notes |
|-----------|-------|-------|
| Pool registry | Marketplace | poolId, contractId, feeBps, navBreakPct |
| Swap router | Marketplace | quote + execute; calls Payment |
| LP positions | Marketplace | shares, deposit/withdraw |
| Circuit breaker | Marketplace + Valuation | listens to `nav.attested` |
| Surveillance | Reporting | pool manipulation alerts |

---

## Track backlog (628–652)

| Track range | Focus |
|-------------|-------|
| 628–632 | Pool entity + create/seed API |
| 633–637 | Swap quote + execute; Payment escrow integration |
| 638–642 | LP deposit/withdraw; fee accounting |
| 643–647 | NAV circuit breaker on `nav.attested` consume |
| 648–652 | Integration tests; investor swap UI; LP admin dashboard |

---

## UI

| Portal | Route | Purpose |
|--------|-------|---------|
| Investor | `/exchange/{contractId}/swap` | Swap USDC ↔ token |
| Admin | `/pools` | Create pool, set breaker threshold, LP overview |

---

## Related docs

- [phase-13-services.md](phase-13-services.md)
- [phase-15-services.md](phase-15-services.md)
- [payment-ledger.md](payment-ledger.md)
