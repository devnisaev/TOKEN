# Payment Ledger — TokenRealty

Adapted from Titan `ledger-accounting.mdc`. Cursor rule: [`.cursor/rules/payment-ledger.mdc`](../../.cursor/rules/payment-ledger.mdc).

**Status:** MVP implemented in `payment-service/` (:8085).

---

## Implemented (MVP)

| Component | Location |
|-----------|----------|
| `Payment`, `Escrow`, `Payout`, `WalletBalance`, `LedgerEntry` | `payment-service/.../entity/` |
| Escrow lifecycle | `PaymentService` — AWAITING_DEPOSIT → HELD → RELEASED / REFUNDED |
| Idempotency | `POST /v1/payments` + `Idempotency-Key` header + unique constraint |
| Ledger entries | `LedgerService` — debit/credit pairs on hold and release |
| Outbox events | `PaymentConfirmedEvent`, `RentCollectedEvent` (typed records) |
| Simulated on-chain | Payout `txHash = 0xSIMULATED_...`; confirm accepts external `txHash` |
| Marketplace escrow | `PaymentClient` initiates on order match; `releaseEscrow` on `transfer.completed` |
| Optional tx verify | `PaymentBlockchainService` — set `PAYMENT_BLOCKCHAIN_ENABLED=true` + `blockchain.rpc-url` |
| Kafka consumer | `OrderMatchedListener` — reconciliation when escrow missing for order |
| Dev auto-confirm | `PaymentAutoConfirmWorker` — `PAYMENT_AUTO_CONFIRM=true` or `local` profile |

## Pending

- Real Web3j USDC/MATIC outbound transfers from Payment Service
- On-chain deposit auto-detection for production (dev uses `PaymentAutoConfirmWorker`)
- On-chain reconciliation job (`WalletBalance` vs chain)
- DLQ for failed consumer retries

---

## Titan vs TokenRealty

| Titan ledger | TokenRealty payment ledger |
|--------------|----------------------------|
| Full double-entry GL (`ISSUER_CASH`, `ATM_CASH`, …) | Escrow-centric: HOLD → RELEASE / REFUND |
| `Hold` domain + GL postings | `Escrow` entity + `LedgerEntry` journal |
| Card auth → hold → capture | Order match → USDC escrow → token transfer → release |
| Redis + SQL hot path | PostgreSQL + on-chain confirmation webhook |
| `Money` type in common lib | `BigDecimal` + `PaymentCurrency` enum |
| Dispute adjustments | Refunds via `PATCH /v1/payments/{id}/refund` |

## Flows

```
Investor → Payment (escrow, sync) → confirm → payment.confirmed (Kafka)
         → Issuance (transfer) → transfer.completed (Kafka)
         → Payment (release escrow) + Marketplace (settle)
Tenant rent → Payment (payout) → Rental confirms (future)
Dividends → Issuance (calculate) → Payment (payout)
```

See [kafka-messaging.md](kafka-messaging.md) for the event-driven buy loop.

### Auto-confirm config (dev)

```yaml
tokenrealty:
  payment:
    auto-confirm:
      enabled: ${PAYMENT_AUTO_CONFIRM:false}
      poll-ms: 3000
```

`PaymentAutoConfirmWorker` calls existing `PaymentService.confirm()` — no duplicate state logic.

See [payment-service/README.md](../../payment-service/README.md) and [PLATFORM-SPEC.md](../PLATFORM-SPEC.md) §4.2.
