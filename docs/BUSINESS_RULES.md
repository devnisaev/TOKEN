# Business Rules (Invariants) — TokenRealty

Tier-1 rules that must not be violated when adding features. Adapted from Titan `business-rules.mdc`; card/ISO/HSM specifics removed.

**Cursor rule:** [`.cursor/rules/business-rules.mdc`](../.cursor/rules/business-rules.mdc)

---

## Related rules (detail by area)

| Area | Human doc | Cursor rule |
|------|-----------|-------------|
| Payment & escrow | [rules/payment-ledger.md](rules/payment-ledger.md) | [payment-ledger.mdc](../.cursor/rules/payment-ledger.mdc) |
| KYC & investment gates | [rules/investment-limits.md](rules/investment-limits.md) | [investment-limits.mdc](../.cursor/rules/investment-limits.mdc) |
| Kafka & outbox | [rules/kafka-messaging.md](rules/kafka-messaging.md) | [kafka-messaging.mdc](../.cursor/rules/kafka-messaging.mdc) |
| Errors & ProblemDetail | — | [business-exception.mdc](../.cursor/rules/business-exception.mdc) |
| Inter-service HTTP | [rules/rest-client-errors.md](rules/rest-client-errors.md) | [rest-client-errors.mdc](../.cursor/rules/rest-client-errors.mdc) |

Event catalog: [EVENTS.md](EVENTS.md). Platform backlog: [PLATFORM-SPEC.md](PLATFORM-SPEC.md).

---

## Never violate

### Money & ledger

- **Traceable money movement** — every payment links `orderId` ↔ `paymentId` ↔ `tradeId` ↔ `txHash` where applicable.
- **Immutable journal** — never UPDATE or DELETE posted `LedgerEntry` rows; corrections = reversal or compensating payment.
- **Balanced entries** — when double-entry is used, sum(debits) = sum(credits) per transaction.
- **Idempotent payments** — same `Idempotency-Key` → same result; same key + different payload → `IdempotencyConflictException` (409).
- **No `double` for money** — use `BigDecimal` with explicit scale; store currency explicitly (`USDC`, etc.).
- **Escrow before transfer** — do not release escrow until `transfer.completed` confirms on-chain (or dev simulated) token movement.

See [payment-ledger.md](rules/payment-ledger.md).

### Compliance & investment

- **KYC before match** — evaluate wallet whitelist via Compliance Service **before** creating a matched buy order or holding escrow.
- **Min investment** — reject orders below `Listing.minInvestmentTokens`.
- **Token stock** — never sell more tokens than `Listing.tokensAvailable`; decrement atomically in the same transaction as order creation.
- **Primary sell-out** — when a PRIMARY listing reaches `tokensAvailable = 0`, sync Registry flat status to `FULLY_SOLD`.
- **Secondary path** — seller KYC + holder balance check before secondary listing; transfer seller → buyer on settle (not SPV → buyer).

See [investment-limits.md](rules/investment-limits.md).

### Kafka & events

- **Outbox in same TX** — enqueue domain events in the same `@Transactional` write as the state change; never call `KafkaTemplate.send` inside an open business transaction.
- **Idempotent consumers** — use `KafkaEventConsumer` + `processed_events` (`eventId` PK); duplicate delivery must not double-apply side effects.
- **One listener → one service call** — parse at the Kafka boundary (`*Command.from(event)`); no business logic in listeners.
- **No long TX with I/O** — listeners must not hold DB transactions while calling HTTP or blockchain; TX on the service method only.

See [kafka-messaging.md](rules/kafka-messaging.md).

### Property & tokenization

- **Registry is source of truth** for buildings, flats, SPV, valuations, and document metadata (`PropertyDocument`).
- **Token info callback** — after on-chain deploy, Issuance updates Registry via `PATCH /v1/flats/{id}/token-info` (contract address, supply, price).
- **Status via service methods** — use `FlatService.updateStatus()` / domain transitions; do not assign lifecycle enums from controllers or Kafka listeners without going through the service.
- **Documents** — store `ipfsCid` on `PropertyDocument` in Registry; upload/pin via Document Service, not ad-hoc CID strings in Registry controllers.

### Security & secrets

- **No private keys or seeds** in logs, Kafka payloads, or API responses.
- **No full KYC document content** in events — reference document IDs / CIDs only.
- **Service-to-service auth** — inter-service RestClients use `ServiceRestClientBuilder` + service JWT; no hardcoded Basic auth in production paths.
- **Secrets from env only** — JWT secret, DB passwords, Pinata JWT, SMTP credentials in environment / `application.yml` placeholders, never committed.

### Errors

- **Business rejections ≠ 500** — use typed exceptions (`ValidationException`, `ResourceNotFoundException`, …) mapped to RFC 7807 ProblemDetail.
- **Raise helpers** — centralize repeated throws in private `raiseValidation` / `raiseNotFound` helpers per service class.

See [business-exception.mdc](../.cursor/rules/business-exception.mdc).

---

## Ordered flows (do not reorder steps)

### Primary market buy (happy path)

```text
1. KYC check (Compliance)     — before order match
2. Reserve tokens (Marketplace) — same TX as order + trade
3. Initiate escrow (Payment)    — sync after match; link paymentId to trade
4. Confirm payment              — on-chain or dev auto-confirm → payment.confirmed
5. Transfer tokens (Issuance)   — PRIMARY: SPV → buyer
6. Settle trade (Marketplace)   — on transfer.completed → release escrow → trade.settled
7. FULLY_SOLD (if applicable)   — Registry flat status when primary listing depleted
```

### Tokenization

```text
1. Flat + SPV verified in Registry
2. Deploy contract (Issuance / Hardhat)
3. PATCH Registry token-info
4. flat.tokenized event → Marketplace auto-listing (when Kafka enabled)
```

### Rent → dividend (when enabled)

```text
1. Record rent (Rental) → Payment payout
2. rent.collected event
3. Issuance DividendService.distribute()
4. dividend.distributed → Payment holder payouts
5. Notification email (optional)
```

---

## Configuration (today vs future)

| Allowed in `application.yml` | Belongs in domain/DB (future admin config) |
|------------------------------|--------------------------------------------|
| Ports, datasource URLs, Kafka bootstrap | Min investment defaults per jurisdiction |
| `KAFKA_ENABLED`, `PAYMENT_AUTO_CONFIRM` | KYC expiry policy thresholds |
| JWT issuer/secret, service account secrets | Notification event toggles per user |
| Inter-service base URLs | MCC-style blocklists (N/A for real estate) |

Do not hardcode business policy constants in service code when they will vary by environment or admin policy. See [PLATFORM-SPEC.md](PLATFORM-SPEC.md) §10 for backlog.

---

## What we skip from Titan

| Titan | TokenRealty |
|-------|-------------|
| PAN/CVV/PIN storage rules | Wallet addresses only; no card data |
| ISO auth sequence (MCC, HSM, stand-in) | Compliance + escrow + transfer sequence above |
| Redis limits engine | DB checks + Compliance API |
| Maker-checker / N-eyes on config | Simple admin JWT roles for now |
| Immutable audit outbox for every mutation | Planned; not platform-wide yet |

---

## Trust nothing external

Every cross-service or on-chain step should be:

**Idempotent · auditable · reversible (where possible) · reconcilable**

When adding a feature, ask: *What happens on retry? Duplicate Kafka message? Partial failure after escrow?*
