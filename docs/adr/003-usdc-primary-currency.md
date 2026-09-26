# ADR 003: USDC as Primary Payment Currency

**Status:** Accepted (2026-09-26)

## Decision

Use **USDC** as the sole primary payment and escrow currency for MVP buy/rent/dividend flows.

## Rationale

- Price stability for real-estate settlement.
- MockUSDC + Payment Service already implement custodial USDC paths.
- Multi-token (MATIC/ETH) adds FX and reconciliation complexity without current product demand.

## Consequences

- `PaymentCurrency.USDC` is default on marketplace initiate and rent payouts.
- On-chain flows use MockUSDC on Hardhat; production targets bridged USDC on Polygon.
