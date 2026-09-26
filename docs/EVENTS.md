# TokenRealty — Kafka Event Catalog

> Canonical list of topics, publishers, consumers, and payload shapes.  
> Conventions: [rules/kafka-messaging.md](rules/kafka-messaging.md) · Spec: [PLATFORM-SPEC.md](PLATFORM-SPEC.md) §6

---

## Envelope (all events)

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "tokenrealty.registry.flat.tokenized.v1",
  "occurredAt": "2025-09-25T16:00:00Z",
  "traceId": "abc-123",
  "payload": { }
}
```

---

## Registry domain

### `tokenrealty.registry.building.approved.v1`

| | |
|---|---|
| **Publisher** | Property Registry |
| **Consumers** | Marketplace, Notification |
| **Partition key** | `buildingId` |
| **When** | Building status → `APPROVED` |

**Payload:**

```json
{
  "buildingId": "uuid",
  "approvedAt": "2025-09-25T16:00:00Z",
  "approvedBy": "user-id-or-service-account"
}
```

---

### `tokenrealty.registry.flat.tokenized.v1`

| | |
|---|---|
| **Publisher** | Property Registry (after Issuance callback) or Token Issuance |
| **Consumers** | Marketplace, Notification |
| **Partition key** | `flatId` |
| **When** | Flat status → `TOKENIZED`; contract deployed |

**Payload:**

```json
{
  "flatId": "uuid",
  "buildingId": "uuid",
  "contractAddress": "0x...",
  "totalTokens": 10000,
  "tokenPriceUsd": "100.00"
}
```

---

## Compliance domain

### `tokenrealty.compliance.investor.kyc-approved.v1`

| | |
|---|---|
| **Publisher** | Compliance Service |
| **Consumers** | Token Issuance |
| **Partition key** | `investorId` |

**Payload:**

```json
{
  "investorId": "uuid",
  "walletAddress": "0x...",
  "approvedAt": "2025-09-25T16:00:00Z"
}
```

---

### `tokenrealty.compliance.investor.kyc-revoked.v1`

| | |
|---|---|
| **Publisher** | Compliance Service |
| **Consumers** | Token Issuance |
| **Partition key** | `investorId` |

**Payload:**

```json
{
  "investorId": "uuid",
  "walletAddress": "0x...",
  "reason": "expired",
  "revokedAt": "2025-09-25T16:00:00Z"
}
```

---

## Rental domain

### `tokenrealty.rental.rent.due.v1`

| | |
|---|---|
| **Publisher** | Rental Service |
| **Consumers** | Notification |
| **Partition key** | `leaseId` |
| **When** | Monthly scheduled job — active lease with no payment recorded for period |

**Payload:**

```json
{
  "leaseId": "uuid",
  "flatId": "uuid",
  "tenantId": "uuid",
  "amountUsd": "1200.00",
  "period": "2025-09",
  "dueDate": "2025-09-01"
}
```

---

### `tokenrealty.rental.lease.expired.v1`

| | |
|---|---|
| **Publisher** | Rental Service |
| **Consumers** | Notification |
| **Partition key** | `leaseId` |
| **When** | Daily job — active lease past `endDate` |

**Payload:**

```json
{
  "leaseId": "uuid",
  "flatId": "uuid",
  "tenantId": "uuid",
  "endDate": "2025-08-31",
  "expiredAt": "2025-09-25T09:00:00Z"
}
```

---

## Marketplace domain

### `tokenrealty.marketplace.listing.created.v1`

| | |
|---|---|
| **Publisher** | Marketplace |
| **Consumers** | Notification |
| **Partition key** | `listingId` |

**Payload:**

```json
{
  "listingId": "uuid",
  "flatId": "uuid",
  "listingType": "PRIMARY",
  "priceUsd": "100.00",
  "tokensAvailable": 5000
}
```

---

### `tokenrealty.marketplace.order.matched.v1`

| | |
|---|---|
| **Publisher** | Marketplace |
| **Consumers** | Payment, Token Issuance |
| **Partition key** | `orderId` |

**Payload:**

```json
{
  "orderId": "uuid",
  "listingId": "uuid",
  "flatId": "uuid",
  "contractId": "uuid",
  "buyerId": "uuid",
  "sellerId": "uuid",
  "tokenAmount": 100,
  "totalPriceUsd": "10000.00"
}
```

---

### `tokenrealty.marketplace.trade.settled.v1`

| | |
|---|---|
| **Publisher** | Marketplace |
| **Consumers** | Notification |
| **Partition key** | `tradeId` |
| **When** | Admin settle or Payment Service confirms payment + transfer |

**Payload:**

```json
{
  "tradeId": "uuid",
  "orderId": "uuid",
  "listingId": "uuid",
  "paymentId": "uuid",
  "transferId": "uuid"
}
```

---

## Payment domain

> **Implementation:** Payment Service MVP publishes via outbox when `tokenrealty.kafka.enabled=true`.  
> Typed payload: `PaymentConfirmedEvent`, `RentCollectedEvent` in `payment-service/.../kafka/events/`.

### `tokenrealty.payment.payment.confirmed.v1`

| | |
|---|---|
| **Publisher** | Payment Service (outbox on `POST /v1/payments/{id}/confirm`) |
| **Consumers** | Token Issuance, Marketplace |
| **Partition key** | `paymentId` |

**Payload:**

```json
{
  "paymentId": "uuid",
  "orderId": "uuid",
  "payerId": "uuid",
  "amount": { "value": "10000.00", "currency": "USDC" },
  "txHash": "0x...",
  "confirmedAt": "2025-09-25T16:00:00Z"
}
```

---

### `tokenrealty.payment.rent.collected.v1`

| | |
|---|---|
| **Publisher** | Payment Service (outbox on `POST /v1/payouts` with purpose RENT) |
| **Consumers** | Rental, Token Issuance |
| **Partition key** | `leaseId` |

**Payload:**

```json
{
  "leaseId": "uuid",
  "flatId": "uuid",
  "tenantId": "uuid",
  "period": "2025-09",
  "amount": { "value": "1500.00", "currency": "USDC" },
  "txHash": "0x...",
  "collectedAt": "2025-09-25T16:00:00Z"
}
```

---

## Issuance domain

### `tokenrealty.issuance.transfer.completed.v1`

| | |
|---|---|
| **Publisher** | Token Issuance |
| **Consumers** | Marketplace, Property Registry, Notification |
| **Partition key** | `contractId` |

**Payload:**

```json
{
  "transferId": "uuid",
  "contractId": "uuid",
  "flatId": "uuid",
  "orderId": "uuid",
  "tradeId": "uuid",
  "paymentId": "uuid",
  "fromWallet": "0x...",
  "toWallet": "0x...",
  "tokenAmount": 100,
  "txHash": "0x...",
  "completedAt": "2025-09-25T16:00:00Z"
}
```

`orderId`, `tradeId`, and `paymentId` link the on-chain transfer to marketplace settlement and escrow release.

---

### `tokenrealty.issuance.dividend.distributed.v1`

| | |
|---|---|
| **Publisher** | Token Issuance |
| **Consumers** | Payment, Notification |
| **Partition key** | `contractId` |

**Payload:**

```json
{
  "contractId": "uuid",
  "flatId": "uuid",
  "totalAmount": { "value": "5000.00", "currency": "USDC" },
  "period": "2025-09",
  "holderPayouts": [
    {
      "dividendPaymentId": "uuid",
      "investorId": "uuid",
      "walletAddress": "0x...",
      "amount": "250.00",
      "ownershipPct": "5.00"
    }
  ],
  "distributedAt": "2025-09-25T16:00:00Z"
}
```

---

### `tokenrealty.payment.payout.completed.v1`

| | |
|---|---|
| **Publisher** | Payment Service (outbox after DIVIDEND payout) |
| **Consumers** | Token Issuance |
| **Partition key** | `payoutId` |

**Payload:**

```json
{
  "payoutId": "uuid",
  "dividendPaymentId": "uuid",
  "recipientInvestorId": "uuid",
  "recipientWallet": "0x...",
  "purpose": "DIVIDEND",
  "txHash": "0x...",
  "completedAt": "2025-09-25T16:00:00Z"
}
```

Issuance updates `DividendPayment.txHash` and status `PAID` on consume.

---

## Document domain

### `tokenrealty.document.document.uploaded.v1`

| | |
|---|---|
| **Publisher** | Document Service |
| **Consumers** | Property Registry, Compliance, Notification |
| **Partition key** | `documentId` |

**Payload:**

```json
{
  "documentId": "uuid",
  "buildingId": "uuid",
  "flatId": "uuid",
  "documentType": "TITLE_DEED",
  "ipfsCid": "Qm...",
  "uploadedAt": "2025-09-25T16:00:00Z"
}
```

---

## DLQ

Failed messages after max retries:

```text
<original-topic>.dlq
```

Example: `tokenrealty.marketplace.order.matched.v1.dlq`

---

## Versioning

| Change type | Action |
|-------------|--------|
| Add optional payload field | OK in `.v1` |
| Rename/remove required field | New topic `.v2` |
| Change semantics | New topic `.v2` |

Update this file when adding or retiring topics.
