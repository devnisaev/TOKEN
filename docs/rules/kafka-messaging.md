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
| Shared parse helper | `KafkaJsonEvent.consume` in `tokenrealty-events` lib |
| DLQ after retries | `<topic>.dlq` |
| Schema evolution | Additive in v1; breaking → v2 topic |
| Dedicated `*KafkaConfig` | `@EnableKafka` + `NewTopic` beans separated from app config |

## What we simplified

| Titan | TokenRealty |
|-------|-------------|
| Full hexagonal `adapter/in/kafka` mandatory | Layered `kafka/` OK for existing services |
| `cardsystem-outbox` shared lib | `tokenrealty-outbox` (`OutboxWriter`, `OutboxPayload`); per-service `outbox_events` table + relay |
| Avro + Schema Registry (implied) | JSON envelope first; Avro optional in Phase 5 |
| 20+ service-specific references | 11 topics in [EVENTS.md](../EVENTS.md) |
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
- [x] Create `tokenrealty-outbox` Maven module (`OutboxWriter`, `OutboxPayload`)
- [x] Add `processed_event` table + repository per consuming service (Marketplace, Payment, Issuance, Notification)
- [x] Add outbox table + relay job per publishing service (Marketplace, Payment, Issuance, Property Registry)

### Phase 1 — First producers (Property Registry)

- [x] `RegistryKafkaEventTypes` class
- [x] `RegistryKafkaConfig` with `NewTopic` beans
- [ ] Outbox publisher for `building.approved`
- [x] Outbox publisher for `flat.tokenized` (Property Registry)
- [x] Emit `flat.tokenized` after DB commit in `FlatService.setTokenInfo()`

### Phase 1 — First consumers (Notification stub)

- [x] `NotificationEventListener` with idempotent `eventId` check (`notification-service`)
- [x] Log-only handler until email provider connected

### Phase 2+ — Commerce & rental

- [x] Marketplace: publish `listing.created`, `order.matched`, `trade.settled` via outbox
- [x] Marketplace: consume `flat.tokenized`, `payment.confirmed`, `transfer.completed`
- [x] Payment: outbox for `payment.confirmed`, `rent.collected` (typed payload records)
- [x] Payment + Marketplace: Kafka relay (`OutboxRelayWorker`)
- [x] Payment: consume `order.matched` (reconciliation)
- [ ] Payment: consume `dividend.distributed`
- [x] Token Issuance: publish `transfer.completed`; consume `payment.confirmed`
- [ ] Token Issuance: publish `dividend.distributed`; consume `kyc-approved`, `rent.collected`

---

## Shared libraries

| Module | Package | Contents |
|--------|---------|----------|
| `tokenrealty-events` | `com.tokenrealty.events` | `EventEnvelope`, `KafkaJsonEvent` (consumer parse helper) |
| `tokenrealty-outbox` | `com.tokenrealty.outbox` | `OutboxWriter` (base enqueue logic), `OutboxPayload` (fluent payload builder) |

Install before service builds:

```bash
./token-realty-app/mvnw -pl tokenrealty-events,tokenrealty-outbox install
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

`OutboxRelayWorker` (`@Scheduled`, `@ConditionalOnProperty tokenrealty.kafka.enabled=true`):

* Polls `PENDING` rows from `outbox_events`
* Publishes envelope JSON to Kafka (`topic = event_type`, key = `aggregate_id`)
* Marks `PUBLISHED` on broker ack; increments `retry_count` / sets `FAILED` after max retries

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

Every consuming service uses the same three-layer shape:

1. **`ProcessedEvent`** entity + `ProcessedEventService.tryClaim(eventId, eventType)` — idempotency before handler runs
2. **`{Service}KafkaIngestSupport`** — wraps `KafkaJsonEvent.consume` + claim
3. **`{Event}Listener`** in `kafka/in/` — `@KafkaListener` → parse command → one `@Service` call

### ProcessedEvent

```text
processed_events (event_id PK, event_type, processed_at)
```

```java
@Transactional
public boolean tryClaim(UUID eventId, String eventType) {
    if (repository.existsByEventId(eventId)) return false;
    try {
        repository.save(new ProcessedEvent(eventId, eventType, Instant.now()));
        return true;
    } catch (DataIntegrityViolationException ex) {
        return false; // concurrent duplicate
    }
}
```

Do **not** inject `ProcessedEventRepository` into business services — only `ProcessedEventService`.

### Ingest support + listener

```java
@Component
@RequiredArgsConstructor
public class MarketplaceKafkaIngestSupport {
    private final ObjectMapper objectMapper;
    private final ProcessedEventService processedEventService;

    public void consume(String message, String eventType, String failure,
                        Consumer<KafkaJsonEvent> handler) {
        KafkaJsonEvent.consume(objectMapper, message, failure, event -> {
            if (!processedEventService.tryClaim(event.eventId(), eventType)) return;
            handler.accept(event);
        });
    }
}

@Component
@ConditionalOnProperty(name = "tokenrealty.kafka.enabled", havingValue = "true")
public class PaymentConfirmedListener {
    @KafkaListener(topics = "${tokenrealty.kafka.topic.payment-confirmed}")
    public void onPaymentConfirmed(String message) {
        ingestSupport.consume(message, MarketplaceKafkaEventTypes.PAYMENT_CONFIRMED,
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

Event-driven primary-market settlement (replaces admin `PATCH /orders/{id}/settle` when Kafka enabled):

```text
1. POST /v1/orders (BUY)     → match + PaymentClient escrow (sync)
2. POST /v1/payments/{id}/confirm → payment.confirmed (outbox → Kafka)
3. Marketplace consumer    → trade.status = PAID
4. Issuance consumer       → TransferService.transfer (on-chain)
                           → transfer.completed (outbox → Kafka)
5. Marketplace consumer    → trade.status = SETTLED + PaymentClient.releaseEscrow
                           → trade.settled event
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
| Marketplace → Issuance | `TokenIssuanceClient` | `GET /v1/tokens/by-flat/{flatId}`, `GET /v1/compliance/check/{wallet}` |

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
├── entity/ProcessedEvent.java
├── service/ProcessedEventService.java
├── client/PaymentClient.java              ← initiate + releaseEscrow
├── kafka/
│   ├── MarketplaceKafkaEventTypes.java
│   ├── MarketplaceKafkaConfig.java
│   ├── command/                           ← PaymentConfirmedCommand, FlatTokenizedCommand, …
│   ├── in/                                ← *Listener, MarketplaceKafkaIngestSupport
│   ├── port/                              ← *Publisher interfaces
│   └── outbox/                            ← OutboxWriter, Outbox*Publisher, OutboxRelayWorker
└── service/
    ├── OrderService.java                  ← onPaymentConfirmed, settleFromTransfer
    └── ListingService.java                ← createFromFlatTokenized
```

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

**Notification (implemented — consumer stub):**

```text
com.tokenrealty.notification/
├── entity/ProcessedEvent.java
├── service/ProcessedEventService.java
├── service/NotificationLogService.java    ← log-only until email provider
├── kafka/
│   ├── NotificationKafkaEventTypes.java
│   ├── NotificationKafkaConfig.java
│   └── in/
│       ├── NotificationKafkaIngestSupport.java
│       └── NotificationEventListener.java   ← multi-topic @KafkaListener
```

No outbox — Notification is consume-only for now.

---

## Local dev flags

| Flag / profile | Service | Effect |
|----------------|---------|--------|
| `KAFKA_ENABLED=true` | All Kafka services | Outbox relay + listeners active |
| `PAYMENT_AUTO_CONFIRM=true` | Payment | Auto-confirms pending payments every 3s |
| `spring.profiles.active=local` | Payment | Enables auto-confirm + Kafka (see `application-local.yml`) |

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
