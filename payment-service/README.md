# Payment Service

Microservice for **TokenRealty** — crypto payments, escrow, payouts, and transaction ledger.

Port **8085** · Database **`payment_service`**

## Responsibilities

- Initiate token purchase payments with escrow (USDC, MATIC, ETH)
- Confirm on-chain deposits and release escrow after token transfer
- Dividend and rent payouts to holder wallets
- Append-only ledger entries for off-chain bookkeeping
- Outbox events: `payment.confirmed`, `rent.collected` (Kafka when enabled)

## Quick start

### 1. Create database

```bash
psql -U postgres -c "CREATE DATABASE payment_service;"
```

### 2. Run the service

From repo root:

```bash
./token-realty-app/mvnw -pl payment-service spring-boot:run
```

### 3. Swagger UI

```
http://localhost:8085/api/swagger-ui.html
```

## Authentication

JWT Bearer from Auth Service (:8083). Login at `POST /api/v1/auth/login`.

| Action | Role |
|--------|------|
| POST `/v1/payments` | INVESTOR, ADMIN |
| POST `/v1/payments/{id}/confirm` | ADMIN, SERVICE |
| PATCH `/v1/payments/{id}/release` | ADMIN |
| POST `/v1/payouts` | ADMIN |

## API overview

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/v1/payments` | Initiate payment + escrow (requires `Idempotency-Key` header) |
| GET | `/v1/payments/{id}` | Payment details with escrow |
| POST | `/v1/payments/{id}/confirm` | Confirm on-chain tx |
| PATCH | `/v1/payments/{id}/release` | Release escrow to SPV |
| PATCH | `/v1/payments/{id}/refund` | Refund escrow |
| POST | `/v1/payouts` | Create dividend/rent payout |
| GET | `/v1/payouts` | List payouts |

## Escrow flow

```
POST /v1/payments → AWAITING_DEPOSIT
POST /v1/payments/{id}/confirm → HELD (+ PaymentConfirmed event)
PATCH /v1/payments/{id}/release → RELEASED
```

## Configuration

```yaml
tokenrealty:
  payment:
    escrow-wallet-address: ${ESCROW_WALLET_ADDRESS:0xEscrowDevWallet...}
  kafka:
    enabled: false
```

## Tests

```bash
./token-realty-app/mvnw -pl payment-service test
```

Platform spec: [docs/PLATFORM-SPEC.md](../docs/PLATFORM-SPEC.md) · Ledger rules: [payment-ledger.mdc](../.cursor/rules/payment-ledger.mdc)
