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
- [ ] Add `processed_event` table + repository per consuming service
- [x] Add outbox table + relay job per publishing service (Marketplace, Payment)

### Phase 1 — First producers (Property Registry)

- [ ] `RegistryKafkaEventTypes` class
- [ ] `RegistryKafkaConfig` with `NewTopic` beans
- [ ] Outbox publisher for `building.approved`, `flat.tokenized`
- [ ] Emit after DB commit in `BuildingService` / `FlatService`

### Phase 1 — First consumers (Notification stub)

- [ ] `NotificationKafkaListener` with idempotent `eventId` check
- [ ] Log-only handler until email provider connected

### Phase 2+ — Commerce & rental

- [x] Marketplace: publish `listing.created`, `order.matched`, `trade.settled` via outbox
- [ ] Marketplace: consume `flat.tokenized`, `payment.confirmed`
- [x] Payment: outbox for `payment.confirmed`, `rent.collected` (typed payload records)
- [x] Payment + Marketplace: Kafka relay (`OutboxRelayWorker`)
- [ ] Payment: consume `order.matched`, `dividend.distributed`
- [ ] Token Issuance: publish `transfer.completed`, `dividend.distributed`; consume `kyc-approved`, `payment.confirmed`, `rent.collected`

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

## Package layout examples

**Property Registry (layered — target):**

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
├── kafka/
│   ├── MarketplaceKafkaEventTypes.java
│   ├── MarketplaceKafkaConfig.java
│   ├── port/
│   │   ├── ListingCreatedPublisher.java
│   │   ├── OrderMatchedPublisher.java
│   │   └── TradeSettledPublisher.java
│   └── outbox/
│       ├── OutboxEvent.java
│       ├── OutboxEventRepository.java
│       ├── OutboxWriter.java
│       ├── OutboxListingCreatedPublisher.java
│       ├── OutboxOrderMatchedPublisher.java
│       ├── OutboxTradeSettledPublisher.java
│       └── OutboxRelayWorker.java
└── service/
    └── OrderService.java                  ← calls port, not OutboxWriter
```

**Payment (implemented):** same shape under `com.tokenrealty.payment.kafka/` with `PaymentConfirmedPublisher`, `RentCollectedPublisher`, and matching `Outbox*` classes.

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
