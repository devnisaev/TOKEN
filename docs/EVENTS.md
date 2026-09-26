# TokenRealty — Kafka Event Catalog

> Canonical list of topics, publishers, consumers, and payload shapes.  
> Conventions: [rules/kafka-messaging.md](rules/kafka-messaging.md) · Spec: [PLATFORM-SPEC.md](PLATFORM-SPEC.md) §6  
> JSON Schema (settlement flow): [schemas/](schemas/)

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
| **Consumers** | Payment, Token Issuance, Reporting, Settlement |
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
| **Consumers** | Notification, Reporting, Settlement |
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

### `tokenrealty.corporateactions.dividend.distribution-requested.v1`

| | |
|---|---|
| **Publisher** | Corporate Actions (outbox) |
| **Consumers** | Token Issuance |
| **Partition key** | `corporateActionId` |

**Payload:**

```json
{
  "corporateActionId": "uuid",
  "flatId": "uuid",
  "contractId": "uuid",
  "period": "2025-09",
  "grossAmountUsd": "1500.00"
}
```

Issuance distributes pro-rata to token holders; `RentCollectedListener` is disabled when Corporate Actions owns the rent → dividend path.

---

### `tokenrealty.issuance.dividend.distributed.v1`

| | |
|---|---|
| **Publisher** | Token Issuance |
| **Consumers** | Payment, Notification, Corporate Actions |
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

---

## Avro serialization (opt-in)

When `tokenrealty.kafka.serialization=avro` is set on a publishing service (Marketplace, Payment):

| Component | Role |
|-----------|------|
| `AvroSchemaRegistrar` | Registers `EventEnvelope` + domain schemas with Apicurio on startup |
| `AvroOutboxPayloadEncoder` | Wraps JSON outbox payload in Avro `EventEnvelope` bytes (base64 in Kafka value) |
| Apicurio (`schema-registry:8080` in compose kafka profile) | Schema Registry compatible with Confluent API |

Consumers remain JSON by default. Enable Avro consumer deserialization only after dual-read validation in staging.

---

## Outbox relay coverage

Publishing services with `OutboxRelayWorker` + integration tests (tracks 253–260):

| Service | Outbox events | IT |
|---------|---------------|-----|
| Marketplace | `listing.created`, `order.matched`, `trade.settled` | `OutboxRelayIntegrationTest` |
| Payment | `payment.confirmed`, `rent.collected`, `payout.completed` | `OutboxRelayIntegrationTest` |
| Compliance | `kyc-approved`, `kyc-revoked` | `OutboxRelayIntegrationTest` |
| Token Issuance | `transfer.completed`, `dividend.distributed` | `OutboxRelayIntegrationTest` |
| Property Registry | `building.approved`, `flat.tokenized` | `OutboxRelayIntegrationTest` |
| Rental | `rent.due`, `lease.expired` | `OutboxRelayIntegrationTest` |
| Document | `document.uploaded` | `OutboxRelayIntegrationTest` + upload IT |
| Blockchain Indexer | `transfer.indexed`, `balance.mismatch` | `OutboxRelayIntegrationTest` |
| Valuation | `valuation.updated` | outbox relay worker (IT pending) |
| Corporate Actions | `dividend.distribution-requested` | outbox relay worker (IT pending) |

Shared test helper: `OutboxKafkaListenerTestConfiguration` in `tokenrealty-kafka` (for services with `@KafkaListener` beans).

---

## Phase 6 — topics (Tier 2 implemented)

See [PLATFORM-SPEC.md §12](PLATFORM-SPEC.md#12-phase-6--planned-services).

| Topic | Publisher | Consumers | Status |
|-------|-----------|-----------|--------|
| `tokenrealty.valuation.updated.v1` | Valuation Service | Registry, Search, Reporting | Implemented (outbox) |
| `tokenrealty.corporateactions.dividend.distribution-requested.v1` | Corporate Actions | Token Issuance | Implemented (outbox) |

**Still planned:**

| Topic (proposed) | Publisher | Consumers | Purpose |
|------------------|-----------|-----------|---------|
| `tokenrealty.valuation.approved.v1` | Valuation Service | Audit Ledger, Notification | Appraisal workflow completion |
| `tokenrealty.settlement.stuck.v1` | Settlement Service | Notification, Reporting | Saga step exceeded SLA |
| `tokenrealty.settlement.recovered.v1` | Settlement Service | Audit Ledger, Notification | Admin retry/compensation succeeded |
| `tokenrealty.audit.entry-recorded.v1` | Audit Ledger | Reporting (optional) | Cross-service audit fan-in |

**Existing topics consumed by Phase 6 services (read-only projections):**

| Consumer service | Topics |
|------------------|--------|
| Reporting | `trade.settled`, `dividend.distributed`, `rent.collected`, `order.matched`, `flat.tokenized`, `building.approved` |
| Settlement Saga | `order.matched`, `payment.confirmed`, `transfer.completed`, `trade.settled`, `payout.completed` |
| Audit Ledger | `kyc-approved`, `kyc-revoked`, `trade.settled`, `document.uploaded`, `order.matched` |
| Corporate Actions | `rent.collected`, `dividend.distributed` |
| Search | `listing.created`, `flat.tokenized`, `building.approved`, `valuation.updated` (implemented) |
