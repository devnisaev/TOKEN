# Kafka Messaging — TokenRealty

Adapted from Titan `kafka-messaging.mdc` for the TokenRealty platform.  
Cursor rule: [`.cursor/rules/kafka-messaging.mdc`](../../.cursor/rules/kafka-messaging.mdc)  
Spring conventions: [spring-java-services.md](spring-java-services.md)

---

## What we took from Titan

| Rule | TokenRealty adaptation |
|------|------------------------|
| Topic naming with version suffix | `tokenrealty.{domain}.{aggregate}.{verb}.v1` |
| Standard JSON envelope | `eventId`, `eventType`, `occurredAt`, `traceId`, `payload` |
| Per-service `*KafkaEventTypes` constants | Same; sync with `application.yml` |
| Transactional outbox | Required for domain events; no `KafkaTemplate` in TX |
| Idempotent consumers | `processed_event` table keyed by `eventId` |
| Listener → one service call | Command/DTO mapping at Kafka boundary |
| Shared parse helper | `KafkaJsonEvent.consume` in `tokenrealty-events`; ingest + idempotency in `tokenrealty-kafka` |
| DLQ after retries | `<topic>.dlq` |
| Schema evolution | Additive in v1; breaking → v2 topic |
| JSON Schema contracts | Settlement-flow payloads in [docs/schemas/](../schemas/) |
| Dedicated `*KafkaConfig` | `@EnableKafka` + `NewTopic` beans separated from app config |

## What we simplified

| Titan | TokenRealty |
|-------|-------------|
| Full hexagonal `adapter/in/kafka` mandatory | Layered `kafka/` OK for existing services |
| `cardsystem-outbox` shared lib | `tokenrealty-outbox` (`OutboxWriter`, `OutboxRelay`, `OutboxPayload`); per-service `outbox_events` table + thin relay worker |
| Avro + Schema Registry | JSON default; Apicurio `:8092` + `AvroEventCodec` opt-in (`docs/schemas/avro/`) |
| 20+ service-specific references | 12+ topics in [EVENTS.md](../EVENTS.md) |
| PCI: never PAN/PIN | Never private keys, seeds, full KYC docs |

## What we skipped

* ISO/card-specific event types and ingest patterns
* Fan-in `*KafkaApplicationService` unless 3+ listeners share orchestration
* `ProcessedEventClaims` for every domain unique insert (use when needed for payments)
* Multi-tenant envelope fields

---

## Implementation checklist

### Phase 1 — Infrastructure

- [x] Add Kafka to root `docker-compose.yml` (`docker compose --profile kafka up`)
- [x] Create `tokenrealty-events` Maven module (envelope record, `KafkaJsonEvent`)
- [x] Create `tokenrealty-outbox` Maven module (`OutboxWriter`, `OutboxPayload`, `OutboxRelay`)
- [x] Create `tokenrealty-kafka` module (`KafkaEventConsumer`, `ProcessedEventClaimService`, shared `ProcessedEvent` entity)
- [x] Shared `processed_events` table via `tokenrealty-kafka` auto-config (no per-service copy)
- [x] Add outbox table + relay job per publishing service (Marketplace, Payment, Issuance, Property Registry)

### Phase 1 — First producers (Property Registry)

- [x] `RegistryKafkaEventTypes` class
- [x] `RegistryKafkaConfig` with `NewTopic` beans
- [x] Outbox publisher for `building.approved`
- [x] Outbox publisher for `flat.tokenized` (Property Registry)
- [x] Emit `flat.tokenized` after DB commit in `FlatService.setTokenInfo()`

### Phase 1 — First consumers (Notification)

- [x] `NotificationEventListener` with idempotent `eventId` check (`notification-service`)
- [x] `NotificationEmailService` — log mode (default) or SMTP (`NOTIFICATION_EMAIL_MODE=smtp`)
- [x] Listeners: flat tokenized, listing created, order matched, payment confirmed, transfer completed
- [x] Listeners: kyc approved/revoked, trade settled, dividend distributed, rent collected

### Phase 2+ — Commerce & rental

- [x] Marketplace: publish `listing.created`, `order.matched`, `trade.settled` via outbox
- [x] Marketplace: consume `flat.tokenized`, `payment.confirmed`, `transfer.completed`
- [x] Payment: outbox for `payment.confirmed`, `rent.collected` (typed payload records)
- [x] Payment + Marketplace: Kafka relay (`OutboxRelayWorker`)
- [x] Payment: consume `order.matched` (reconciliation)
- [x] Payment: consume `dividend.distributed` → create `DIVIDEND` payouts per holder
- [x] Token Issuance: publish `transfer.completed`; consume `payment.confirmed`
- [x] Token Issuance: publish `dividend.distributed`; consume `rent.collected` (Kafka → `DividendService.distribute()`)
- [x] Compliance: publish `kyc-approved`, `kyc-revoked` via outbox
- [x] Token Issuance: consume `kyc-approved`, `kyc-revoked` → on-chain whitelist sync
- [x] Document: publish `document.uploaded` via outbox after Registry callback
- [x] Shared DLQ error handler (`tokenrealty-kafka` `KafkaDlqAutoConfiguration`) — opt-in via `tokenrealty.kafka.dlq.enabled=true`
- [x] Local profile DLQ enabled for marketplace, payment, issuance, compliance, notification, document (`application-local.yml`)

Config keys: `tokenrealty.kafka.dlq.enabled`, `max-retries` (default 3), `backoff-ms` (default 1000), `suffix` (default `.dlq`). Failed listener messages publish to `<topic>.dlq` after retries.

---

## Shared libraries

| Module | Package | Contents |
|--------|---------|----------|
| `tokenrealty-events` | `com.tokenrealty.events` | `EventEnvelope`, `KafkaJsonEvent` (consumer parse helper) |
| `tokenrealty-kafka` | `com.tokenrealty.kafka` | `KafkaEventConsumer`, `ProcessedEventClaimService`, `ProcessedEvent` entity |
| `tokenrealty-outbox` | `com.tokenrealty.outbox` | `OutboxWriter`, `OutboxPayload`, `OutboxRelay`, `OutboxStatus` |
| `tokenrealty-web` | `com.tokenrealty.web` | Exceptions, RFC 7807 handler, observability, outbound REST helpers |
| `tokenrealty-jpa` | `com.tokenrealty.jpa` | `BaseEntity`, `@EnableJpaAuditing` auto-config |
| `tokenrealty-security` | `com.tokenrealty.security` | JWT, service tokens, `ServiceRestClientBuilder` |

Install before service builds:

```bash
./token-realty-app/mvnw -pl tokenrealty-security,tokenrealty-web,tokenrealty-jpa,tokenrealty-kafka,tokenrealty-events,tokenrealty-outbox install
```

Each publishing service adds a **concrete** `@Service OutboxWriter` in `kafka/outbox/` that extends `com.tokenrealty.outbox.OutboxWriter` and persists to its local `outbox_events` table.

---

## Outbox publishing pattern

Adapted from Titan `cardsystem-outbox`. Three layers — do not skip:

1. **Port** — application depends on interface + typed event record (`kafka/port/`)
2. **Outbox adapter** — one `@Component` per topic (`Outbox{Event}Publisher` in `kafka/outbox/`)
3. **Writer** — `@Service OutboxWriter` persists envelope JSON in the same TX as domain writes

### Port + typed event

```java
public interface OrderMatchedPublisher {
    void publishOrderMatched(OrderMatchedEvent event);

    record OrderMatchedEvent(UUID orderId, UUID tradeId, UUID listingId, ...) {}
}
```

Service builds the record and calls the port — never `OutboxWriter` or `KafkaTemplate` directly:

```java
orderMatchedPublisher.publishOrderMatched(new OrderMatchedPublisher.OrderMatchedEvent(
        order.getId(), trade.getId(), ...));
```

### Outbox adapter (one class per topic)

```java
@Component
@RequiredArgsConstructor
public class OutboxOrderMatchedPublisher implements OrderMatchedPublisher {

    private final OutboxWriter outboxWriter;

    @Value("${tokenrealty.kafka.topic.order-matched:" + MarketplaceKafkaEventTypes.ORDER_MATCHED + "}")
    private String orderMatchedTopic;

    @Override
    public void publishOrderMatched(OrderMatchedEvent event) {
        OutboxPayload.start()
                .put("orderId", event.orderId())
                .put("tradeId", event.tradeId())
                .putIfPresent("paymentId", event.paymentId())
                .enqueue(outboxWriter, orderMatchedTopic, event.orderId());
    }
}
```

Rules:

* One `Outbox{Event}Publisher` per topic — do not merge unrelated events into one class.
* Topic from `@Value` with `*KafkaEventTypes` constant as default (keep in sync with `application.yml`).
* Partition key = primary business ID (`orderId`, `paymentId`, `listingId`, …).
* Use `OutboxPayload.putIfPresent` / `putIfNotNull` for optional fields; never put `null` in the map.
* Money → `put("amount", bigDecimal)` or nested `Map.of("value", …, "currency", …)` — never raw `float`.

### OutboxWriter

Base class in `tokenrealty-outbox` wraps payload in `EventEnvelope` and calls abstract `persistOutboxEvent`:

```java
@Transactional
public void enqueue(String eventType, String partitionKey, Object payload, String traceId) {
    enqueueWithEventId(eventType, partitionKey, payload, traceId, UUID.randomUUID());
}

@Transactional
public void enqueueWithEventId(..., UUID eventId) {
    String json = objectMapper.writeValueAsString(
            EventEnvelope.ofWithEventId(eventId, eventType, traceId, payload));
    persistOutboxEvent(eventType, partitionKey, json, traceId, eventId, Instant.now());
}
```

Service subclass (example: Marketplace):

```java
@Service
public class OutboxWriter extends com.tokenrealty.outbox.OutboxWriter {
    // OutboxEventRepository + persistOutboxEvent → outbox_events row (status PENDING)
}
```

When `tokenrealty.kafka.enabled=false` (default in dev/tests), the service `OutboxWriter` no-ops — publishers stay simple with no `if (kafkaEnabled)` checks.

### Outbox relay

Per-service `OutboxRelayWorker` delegates to shared `OutboxRelay.relay(...)` (`@Scheduled`, `@ConditionalOnProperty tokenrealty.kafka.enabled=true`):

* Polls `PENDING` rows from local `outbox_events`
* Publishes envelope JSON to Kafka (`topic = event_type`, key = `aggregate_id`)
* Marks `PUBLISHED` on broker ack; increments `retry_count` / sets `FAILED` after max retries

`OutboxEvent` implements `OutboxRelayTarget` and uses shared `OutboxStatus`.

Config (`application.yml`):

```yaml
tokenrealty:
  kafka:
    enabled: ${KAFKA_ENABLED:false}
    relay:
      poll-ms: 1000
      publish-timeout-ms: 10000
      max-retries: 5
    topic:
      order-matched: tokenrealty.marketplace.order.matched.v1
```

Local Kafka: `docker compose --profile kafka up` from `token-realty-app/`.

---

## Consumer pattern (implemented)

Every consuming service uses the same two-layer shape (idempotency + ingest from `tokenrealty-kafka`):

1. **`KafkaEventConsumer`** — wraps `KafkaJsonEvent.consume` + `ProcessedEventClaimService.tryClaim`
2. **`{Event}Listener`** in `kafka/in/` — `@KafkaListener` → parse command → one `@Service` call

See [shared-libraries.md](shared-libraries.md) for module setup.

### ProcessedEvent (shared)

```text
processed_events (event_id PK, event_type, processed_at)
```

Entity and `ProcessedEventClaimService` live in `tokenrealty-kafka`. Do **not** copy them into services.

### Listener pattern

```java
@Component
@ConditionalOnProperty(name = "tokenrealty.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
public class PaymentConfirmedListener {

    private final KafkaEventConsumer eventConsumer;
    private final OrderService orderService;

    @KafkaListener(topics = "${tokenrealty.kafka.topic.payment-confirmed}")
    public void onPaymentConfirmed(String message) {
        eventConsumer.consume(message, MarketplaceKafkaEventTypes.PAYMENT_CONFIRMED,
                "Payment confirmed processing failed",
                event -> orderService.onPaymentConfirmed(PaymentConfirmedCommand.from(event)));
    }
}
```

Commands live in `kafka/command/` — static `from(KafkaJsonEvent event)` factory, no parse logic in listeners.

Rules:

* `@ConditionalOnProperty(tokenrealty.kafka.enabled=true)` on every listener class.
* **No** `@Transactional` on listener methods that trigger HTTP or blockchain — keep TX on the service method only.
* HTTP/blockchain after DB commit: e.g. `TransferCompletedListener` calls `orderService.settleFromTransfer()` then `paymentClient.releaseEscrow()` outside the settle TX.
* Add consumed topics to `*KafkaConfig` (`NewTopic` beans) and `application.yml` even when this service does not publish them.

---

## Automated buy flow (implemented)

Event-driven settlement for primary and secondary listings (replaces admin `PATCH /orders/{id}/settle` when Kafka enabled):

```text
1. POST /v1/orders (BUY)     → KYC via Compliance; match + PaymentClient escrow (sync)
2. POST /v1/payments/{id}/confirm → payment.confirmed (outbox → Kafka)
3. Marketplace consumer    → trade.status = PAID
4. Issuance consumer       → TransferService.transfer (on-chain)
                             PRIMARY: SPV → buyer | SECONDARY: seller → buyer
                           → transfer.completed (outbox → Kafka)
5. Marketplace consumer    → trade.status = SETTLED + PaymentClient.releaseEscrow
                           → trade.settled event
6. Primary sell-out        → PATCH Registry flat status FULLY_SOLD (sync, on step 1 when tokensAvailable = 0)
```

KYC verify/revoke (Compliance → Issuance):

```text
1. PATCH /v1/compliance/{id}/verify  → kyc-approved (outbox → Kafka)
2. Issuance consumer                 → OnChainWhitelistService.whitelist(wallet)
3. PATCH /v1/compliance/{id}/revoke  → kyc-revoked (outbox → Kafka)
4. Issuance consumer                 → OnChainWhitelistService.revoke(wallet)
```

| Step | Topic | Consumer service | Handler |
|------|-------|------------------|---------|
| Escrow on match | — | Marketplace (sync) | `PaymentClient.initiateTokenPurchase` |
| Payment confirmed | `payment.confirmed` | Marketplace | `OrderService.onPaymentConfirmed` |
| Payment confirmed | `payment.confirmed` | Issuance | `PaymentTransferService.executeTransfer` |
| Transfer done | `transfer.completed` | Marketplace | `OrderService.settleFromTransfer` + `PaymentClient.releaseEscrow` |
| Auto listing | `flat.tokenized` | Marketplace | `ListingService.createFromFlatTokenized` |
| Reconciliation | `order.matched` | Payment | `OrderEscrowService.ensureEscrowLinked` |

Inter-service REST (service JWT):

| Caller | Client | Endpoint |
|--------|--------|----------|
| Issuance → Marketplace | `MarketplaceClient` | `GET /v1/orders/{id}/trade` |
| Marketplace → Payment | `PaymentClient` | `POST /v1/payments`, `PATCH /v1/payments/{id}/release` |
| Marketplace → Compliance | `ComplianceClient` | `GET /v1/compliance/check/{wallet}` |
| Marketplace → Registry | `PropertyRegistryClient` | `PATCH /v1/flats/{id}/status?status=FULLY_SOLD` (primary sell-out) |
| Marketplace → Issuance | `TokenIssuanceClient` | `GET /v1/tokens/by-flat/{flatId}`, `GET /v1/tokens/{contractId}/holders/by-wallet/{wallet}` |
| Document → Registry | `PropertyRegistryClient` | `POST /v1/buildings/{id}/documents`, `POST /v1/flats/{id}/documents`, `GET /v1/documents/{id}` |
| Issuance → Compliance | `ComplianceClient` | `GET /v1/compliance/check/{wallet}` (transfer KYC gate) |
| Wallet → Payment | `PaymentClient` | `GET /v1/wallet-balances/{investorId}` |
| Wallet → Issuance | `IssuanceClient` | `GET /v1/investors/{investorId}/holdings` |
| Indexer → Issuance | `IssuanceClient` | `GET /v1/tokens`, `GET /v1/tokens/{id}/holders` |

**Dev auto-confirm:** `PaymentAutoConfirmWorker` polls `PENDING` payments when `tokenrealty.payment.auto-confirm.enabled=true` (`PAYMENT_AUTO_CONFIRM` env or `local` profile). Uses `0xSIMULATED_{paymentId}` tx hash. Production still needs on-chain deposit detection.

---

## Package layout examples

**Property Registry (implemented):**

```text
com.tokenrealty.registry/
├── kafka/
│   ├── RegistryKafkaEventTypes.java
│   ├── RegistryKafkaConfig.java
│   ├── port/
│   │   └── FlatTokenizedPublisher.java
│   └── outbox/
│       ├── OutboxEvent.java
│       ├── OutboxEventRepository.java
│       ├── OutboxWriter.java              ← extends com.tokenrealty.outbox.OutboxWriter
│       ├── OutboxFlatTokenizedPublisher.java
│       └── OutboxRelayWorker.java
```

**Marketplace (implemented):**

```text
com.tokenrealty.marketplace/
├── client/PaymentClient.java              ← initiate + releaseEscrow
├── kafka/
│   ├── MarketplaceKafkaEventTypes.java
│   ├── MarketplaceKafkaConfig.java
│   ├── command/                           ← PaymentConfirmedCommand, FlatTokenizedCommand, …
│   ├── in/                                ← *Listener (inject KafkaEventConsumer)
│   ├── port/                              ← *Publisher interfaces
│   └── outbox/                            ← OutboxWriter, Outbox*Publisher, OutboxRelayWorker
└── service/
    ├── OrderService.java                  ← onPaymentConfirmed, settleFromTransfer
    └── ListingService.java                ← createFromFlatTokenized
```

Idempotency: `tokenrealty-kafka` (`ProcessedEvent` + `ProcessedEventClaimService`).

**Token Issuance (implemented):**

```text
com.tokenrealty.issuance/
├── client/MarketplaceClient.java          ← GET trade by orderId
├── kafka/
│   ├── IssuanceKafkaEventTypes.java
│   ├── IssuanceKafkaConfig.java
│   ├── command/PaymentConfirmedCommand.java
│   ├── in/PaymentConfirmedListener.java
│   ├── port/TransferCompletedPublisher.java
│   └── outbox/                            ← OutboxTransferCompletedPublisher, relay
└── service/PaymentTransferService.java    ← transfer on payment.confirmed
```

**Payment (implemented):** outbox publishers + `kafka/in/OrderMatchedListener` for escrow reconciliation.

**Document (implemented — publisher):**

```text
com.tokenrealty.document/
├── client/PropertyRegistryClient.java     ← register document after IPFS pin
├── storage/IpfsStorageService.java        ← simulated dev CID or Pinata API
├── service/DocumentUploadService.java
├── controller/DocumentController.java     ← POST /v1/documents/upload, GET /v1/documents/{id}
└── kafka/
    ├── DocumentKafkaEventTypes.java
    ├── DocumentKafkaConfig.java
    ├── port/DocumentUploadedPublisher.java
    └── outbox/                            ← OutboxWriter, OutboxDocumentUploadedPublisher, relay
```

Upload flow: multipart → IPFS pin → Registry `PropertyDocument` with `ipfsCid` → outbox `document.uploaded` → Registry ack + Compliance review queue + Notification email.

**Notification (implemented — consumer + email):**

```text
com.tokenrealty.notification/
├── service/
│   ├── NotificationLogService.java        ← ingest + dispatch
│   └── NotificationEmailService.java      ← log mode or SMTP
├── config/NotificationMailConfig.java     ← JavaMailSender when mode=smtp
└── kafka/
    ├── NotificationKafkaEventTypes.java
    ├── NotificationKafkaConfig.java
    └── in/NotificationEventListener.java  ← multi-topic @KafkaListener + KafkaEventConsumer
```

No outbox — Notification is consume-only.

**Phase 6 services (implemented)** — see [phase-6-services.md](phase-6-services.md):

| Service | Outbox events | Consumer topics |
|---------|---------------|-----------------|
| Reporting | — | `trade.settled`, `order.matched`, `dividend.distributed`, `rent.collected`, `flat.tokenized` |
| Settlement | `settlement.stuck`, `settlement.recovered` | `order.matched`, `payment.confirmed`, `transfer.completed`, `trade.settled` |
| Valuation | `valuation.updated` | — |
| Audit Ledger | — | `kyc-approved`, `kyc-revoked`, `trade.settled`, `document.uploaded`, `order.matched` |
| Corporate Actions | `dividend.distribution-requested` | `rent.collected`, `dividend.distributed` |
| Search | — | `listing.created`, `flat.tokenized`, `building.approved`, `valuation.updated` |
| Integration Hub | — | Webhook relay only (no Kafka) |

**Rent → dividend (Corporate Actions path):** disable Issuance `RentCollectedListener` in local/prod when Hub path active (`tokenrealty.dividend.rent-collected-listener-enabled: false`).

**Phase 7 event mesh (implemented):** Registry ← `valuation.updated`; Notification/Reporting/Audit ← `settlement.stuck`, `settlement.recovered`, `valuation.approved`. See [phase-7-services.md](phase-7-services.md).

Email config:

```yaml
tokenrealty:
  notification:
    email:
      enabled: true
      mode: log          # or smtp
      default-recipient: admin@tokenrealty.com
```

---

## Local dev flags

| Flag / profile | Service | Effect |
|----------------|---------|--------|
| `KAFKA_ENABLED=true` | All Kafka services | Outbox relay + listeners active |
| `PAYMENT_AUTO_CONFIRM=true` | Payment | Auto-confirms pending payments every 3s |
| `spring.profiles.active=local` | Payment | Enables auto-confirm + Kafka (see `application-local.yml`) |
| `IPFS_MODE=simulated` (default) | Document | SHA-256 based dev CID; set `pinata` + `PINATA_JWT` for real pins |
| `NOTIFICATION_EMAIL_MODE=log` (default) | Notification | Logs email body; `smtp` uses `spring.mail.*` |

---

## Testing

```java
// Consumer idempotency test
@Test
void skipsDuplicateEventId() {
    send(flatTokenizedEvent);
    send(flatTokenizedEvent); // same eventId
    verify(marketplaceService, times(1)).handleFlatTokenized(any());
}

// Outbox + relay integration test (Testcontainers)
@Test
void persistsOutboxInSameTransactionAsFlatUpdate() {
    flatService.setTokenInfo(flatId, ...);
    assertThat(outboxRepository.findUnpublished()).hasSize(1);
}
```

---

## Related docs

* [EVENTS.md](../EVENTS.md) — topic catalog and payloads
* [PLATFORM-SPEC.md](../PLATFORM-SPEC.md) §6 — architecture context
* [diagrams/06-kafka-events.puml](../diagrams/06-kafka-events.puml) — topology diagram
