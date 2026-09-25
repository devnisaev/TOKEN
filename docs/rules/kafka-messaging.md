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
| `cardsystem-outbox` shared lib | Start per-service outbox table; extract shared lib later |
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

- [ ] Add Kafka to root `docker-compose.yml`
- [ ] Create `tokenrealty-events` Maven module (envelope record, `KafkaJsonEvent`, topic constants)
- [ ] Add `processed_event` table + repository per consuming service
- [ ] Add outbox table + relay job per publishing service

### Phase 1 — First producers (Property Registry)

- [ ] `RegistryKafkaEventTypes` class
- [ ] `RegistryKafkaConfig` with `NewTopic` beans
- [ ] Outbox publisher for `building.approved`, `flat.tokenized`
- [ ] Emit after DB commit in `BuildingService` / `FlatService`

### Phase 1 — First consumers (Notification stub)

- [ ] `NotificationKafkaListener` with idempotent `eventId` check
- [ ] Log-only handler until email provider connected

### Phase 2+ — Commerce & rental

- [ ] Marketplace: publish `listing.created`, `order.matched`; consume `flat.tokenized`, `payment.confirmed`
- [ ] Payment: publish `payment.confirmed`, `rent.collected`; consume `order.matched`, `dividend.distributed`
- [ ] Token Issuance: publish `transfer.completed`, `dividend.distributed`; consume `kyc-approved`, `payment.confirmed`, `rent.collected`

---

## Package layout examples

**Property Registry (layered):**

```text
com.tokenrealty.registry/
├── kafka/
│   ├── RegistryKafkaEventTypes.java
│   ├── RegistryKafkaConfig.java
│   ├── outbox/
│   │   ├── OutboxEvent.java
│   │   ├── OutboxRepository.java
│   │   ├── RegistryEventPublisher.java
│   │   └── OutboxRelayScheduler.java
│   └── (no consumers in registry initially)
```

**Marketplace (new service, light hexagonal):**

```text
com.tokenrealty.marketplace/
├── adapter/in/kafka/
│   ├── MarketplaceKafkaListener.java
│   └── MarketplaceKafkaIngestSupport.java
├── adapter/out/kafka/
│   └── MarketplaceOutboxPublisher.java
└── application/
    ├── command/
    └── service/
```

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
