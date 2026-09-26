# ADR 004: Hybrid Custodial + WalletConnect Wallet Model

**Status:** Accepted (2026-09-26)

## Decision

Adopt a **hybrid wallet model**: custodial USDC balances in Payment Service for buy/rent, plus optional **WalletConnect** linking for on-chain token holdings and signing.

## Rationale

- Custodial path enables demo buy-flow without requiring users to hold gas or USDC on-chain first.
- WalletConnect supports non-custodial investor identity and future on-chain signing.
- Wallet Service stores encrypted keys with local/KMS/AWS/GCP adapters.

## Consequences

- Investor portal supports MetaMask inject + WalletConnect (`VITE_WALLETCONNECT_PROJECT_ID`).
- Payment escrow can draw from custodial balance before chain deposit confirmation.
