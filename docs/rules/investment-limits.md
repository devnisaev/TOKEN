# Investment & Compliance Limits — TokenRealty

Adapted from Titan `limits-engine.mdc`. Cursor rule: [`.cursor/rules/investment-limits.mdc`](../../.cursor/rules/investment-limits.mdc).

---

## Titan vs TokenRealty

| Titan limits engine | TokenRealty |
|---------------------|-------------|
| Dedicated `limits-service` | Rules split across Marketplace, Issuance, future Compliance |
| Redis Lua atomic reserve | DB checks + compliance API (no sub-ms auth hot path) |
| Multi-dimensional policy (daily, MCC, country) | KYC whitelist + listing min investment + token availability |
| ISO decline codes 61/65/57 | RFC 7807 `ValidationException` / `ComplianceBlockedException` |
| Evaluate before ledger hold | KYC before order match; payment check before escrow release |
| `limit_policy` scoped table | `Listing.minInvestmentTokens`; future `compliance-service` policies |

## What we keep from Titan

- **Evaluate before money moves** — compliance before match, not after
- **Most restrictive wins** — e.g. min investment vs available tokens
- **Typed decline** — business exceptions, not generic errors
- **Idempotent reversal** — cancel order restores tokens; escrow released via Payment
- **Policy in DB** (future) — not hardcoded in controllers

## Current implementation

| Check | Service | API / field |
|-------|---------|-------------|
| KYC whitelist | Token Issuance | `GET /v1/compliance/check/{wallet}` |
| Min investment | Marketplace | `Listing.minInvestmentTokens` |
| Token stock | Marketplace | `Listing.tokensAvailable` |
| Role-based access | All | JWT + `@PreAuthorize` |

## Future (Compliance Service :8087)

- Jurisdiction restrictions (country-specific investment rules)
- Accredited investor verification
- Periodic KYC re-verification and revocation sync to Issuance whitelist

No separate Redis limits engine planned unless high-frequency trading volume requires it.
