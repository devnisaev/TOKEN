# Payment Ledger — TokenRealty

Adapted from Titan `ledger-accounting.mdc`. Cursor rule: [`.cursor/rules/payment-ledger.mdc`](../../.cursor/rules/payment-ledger.mdc).

**Status:** Forward-looking — applies when `payment-service/` is built (:8085).

---

## Titan vs TokenRealty

| Titan ledger | TokenRealty payment ledger |
|--------------|----------------------------|
| Full double-entry GL (`ISSUER_CASH`, `ATM_CASH`, …) | Escrow-centric: HOLD → RELEASE / REFUND |
| `Hold` domain + GL postings | `Escrow` entity + optional `LedgerEntry` journal |
| Card auth → hold → capture | Order match → USDC escrow → token transfer → release |
| Redis + SQL hot path | PostgreSQL + on-chain confirmation webhook |
| `Money` type in common lib | `BigDecimal` + currency enum |
| Flyway seed system accounts | Liquibase + enum/reference table for system buckets |
| Dispute adjustments | Refunds / admin reversal via new payment records |

## What we keep from Titan

- **Invariant:** debits = credits per transaction (when using journal entries)
- **Append-only** ledger entries — no UPDATE/DELETE on posted rows
- **Idempotency** before money side effects (`Idempotency-Key`, dedupe table)
- **Optimistic locking** (`@Version`) on balance/escrow aggregates
- **Corrections via reversal** — not in-place edits
- **Same TX** for domain write + outbox enqueue
- **No negative balance** without explicit credit line

## TokenRealty-specific flows

```
Investor → Payment (escrow HOLD) → Issuance (transfer) → Payment (RELEASE) → Marketplace (settle)
Tenant rent → Payment → SPV wallet → Rental confirms
Dividends → Issuance (calculate) → Payment (Payout to holders)
```

## Entities (planned)

`Payment`, `Escrow`, `Payout`, `LedgerEntry`, `WalletBalance` — see [PLATFORM-SPEC.md](../PLATFORM-SPEC.md) §4.2.

## Reconciliation

Blockchain Indexer (Phase 5) compares on-chain USDC balance vs off-chain `WalletBalance` / holder registry.
