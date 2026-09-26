# Investment & Compliance Limits — TokenRealty

Adapted from Titan `limits-engine.mdc`. Cursor rule: [`.cursor/rules/investment-limits.mdc`](../../.cursor/rules/investment-limits.mdc).

---

## Titan vs TokenRealty

| Titan limits engine | TokenRealty |
|---------------------|-------------|
| Dedicated `limits-service` | Rules split across Marketplace, Compliance, Issuance (on-chain sync) |
| Redis Lua atomic reserve | DB checks + compliance API (no sub-ms auth hot path) |
| Multi-dimensional policy (daily, MCC, country) | KYC whitelist + listing min investment + token availability |
| ISO decline codes 61/65/57 | RFC 7807 `ValidationException` / `ComplianceBlockedException` |
| Evaluate before ledger hold | KYC before order match; payment check before escrow release |
| `limit_policy` scoped table | `Listing.minInvestmentTokens`; future Compliance policy table |

## What we keep from Titan

- **Evaluate before money moves** — compliance before match, not after
- **Most restrictive wins** — e.g. min investment vs available tokens
- **Typed decline** — business exceptions, not generic errors
- **Idempotent reversal** — cancel order restores tokens; escrow released via Payment
- **Policy in DB** (future) — not hardcoded in controllers

## Current implementation

| Check | Service | API / field |
|-------|---------|-------------|
| KYC whitelist | Compliance (:8087) | `GET /v1/compliance/check/{wallet}` → `isWhitelisted`, `investorId` |
| KYC on-chain sync | Token Issuance | Consumes `kyc-approved` / `kyc-revoked` → `OnChainWhitelistService` |
| Min investment | Marketplace | `Listing.minInvestmentTokens` |
| Token stock | Marketplace | `Listing.tokensAvailable` |
| Secondary seller balance | Marketplace → Issuance | `GET /v1/tokens/{contractId}/holders/by-wallet/{wallet}` |
| Secondary seller KYC | Marketplace → Compliance | Same check endpoint; also validated on buy against listing seller |
| Payment idempotency | Payment | `Idempotency-Key` on `POST /v1/payments` |
| Escrow before transfer | Payment | `POST /v1/payments` on match; release after `transfer.completed` |
| Event-driven settle | Marketplace + Issuance | Kafka: `payment.confirmed` → transfer → `transfer.completed` |
| Secondary transfer path | Token Issuance | `PaymentTransferService`: `SECONDARY` → seller wallet; `PRIMARY` → SPV wallet |
| Primary sell-out status | Marketplace → Registry | When `tokensAvailable = 0` on PRIMARY buy → `PATCH /v1/flats/{id}/status?status=FULLY_SOLD` |
| Dev payment confirm | Payment | `PAYMENT_AUTO_CONFIRM=true` — auto-confirms pending escrow |
| Role-based access | All | JWT + `@PreAuthorize`; SERVICE role for inter-service release |

### Secondary sell flow (MVP)

1. `POST /v1/listings/secondary` or `POST /v1/orders/sell` — seller KYC + holder balance check, creates `SECONDARY` listing
2. Buyer uses existing `POST /v1/orders` (buy) — buyer KYC + seller KYC if secondary listing
3. Settlement same as primary: escrow → `payment.confirmed` → on-chain transfer (seller → buyer) → `transfer.completed`

No order book — sell creates listing inventory; buy consumes it (same model as primary).

## Future (Compliance Service :8087)

- Jurisdiction restrictions (country-specific investment rules)
- Accredited investor verification
- Periodic KYC re-verification policies (revocation already syncs via `kyc-revoked` event)

No separate Redis limits engine planned unless high-frequency trading volume requires it.
