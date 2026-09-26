---
name: kafka-event-architect
description: Design and review event-driven messaging structures for TokenRealty microservices. Use when defining Kafka topics, outbox events, payload records, or consumer listeners.
---

# Kafka Event Architect (TokenRealty)

Maintain strict event-driven decoupling across TokenRealty services using the transactional outbox pattern.

## Core Rules

1. **Topic Naming:** Strictly follow the format: `tokenrealty.{domain}.{event-name-kebab}` (e.g., `tokenrealty.marketplace.order-matched`).
2. **Transactional Outbox:** Write outbox events within the exact same database transaction as domain state mutations to ensure zero-loss messaging.
3. **Payload Contracts:** Use Java immutable `record` types for event payloads. Never publish raw entities or domain models directly to Kafka.
4. **Consumer Idempotency:** Consumers must implement deduplication or idempotent processing handlers using unique event or transaction IDs.
5. **Schema Evolution:** Ensure backward compatibility when modifying event fields; use optional fields or version tags if breaking changes are required.

## Forbidden

- Direct publishing to Kafka without an outbox table backup.
- Unhandled JSON parsing exceptions crashing listener threads without dead-letter routing.

---

## Canonical reference (do not duplicate here)

Full conventions, code patterns, and service examples live in:

| Doc | Purpose |
|-----|---------|
| [.cursor/rules/kafka-messaging.mdc](../../rules/kafka-messaging.mdc) | Outbox, listeners, relay, idempotency |
| [docs/rules/kafka-messaging.md](../../../docs/rules/kafka-messaging.md) | Human-readable expansion |
| [docs/EVENTS.md](../../../docs/EVENTS.md) | Topic catalog + JSON payload schemas |
| [docs/BUSINESS_RULES.md](../../../docs/BUSINESS_RULES.md) | Outbox-in-same-TX invariant |

Topic pattern in production:

```text
tokenrealty.{domain}.{aggregate}.{past-tense-verb}.v1
```

Example: `tokenrealty.marketplace.order.matched.v1`

## Project layout (per publishing service)

```text
kafka/
├── {Service}KafkaEventTypes.java     ← topic constants
├── {Service}KafkaConfig.java         ← @EnableKafka + NewTopic beans
├── port/*Publisher.java              ← interface + typed event record
├── outbox/
│   ├── OutboxEvent.java
│   ├── OutboxEventRepository.java
│   ├── OutboxWriter.java             ← extends com.tokenrealty.outbox.OutboxWriter
│   ├── Outbox{Event}Publisher.java   ← one class per topic
│   └── OutboxRelayWorker.java
└── in/*Listener.java                 ← consumers only
```

Consumers use `KafkaEventConsumer` + `processed_events` from `tokenrealty-kafka` — do not copy idempotency tables per service.

## Workflow — add a new published event

1. Define payload in [docs/EVENTS.md](../../../docs/EVENTS.md) (partition key, JSON example).
2. Add constant to `*KafkaEventTypes` and `application.yml` under `tokenrealty.kafka.topic.*`.
3. Add `NewTopic` bean in `*KafkaConfig` (when `tokenrealty.kafka.enabled=true`).
4. Create port interface + typed `record` for the event.
5. Implement `Outbox{Event}Publisher` using `OutboxPayload.start()...enqueue()`.
6. Call publisher from `@Transactional` service method — same TX as domain write.
7. Add unit test; integration test with `KAFKA_ENABLED=false` (outbox no-ops by default).

## Workflow — add a new consumer

1. Add topic to listener's `application.yml` and `NotificationKafkaConfig` / service `*KafkaConfig` if needed.
2. Create `kafka/command/*Command.java` with `static from(KafkaJsonEvent event)`.
3. Add `@KafkaListener` class in `kafka/in/` — delegate to `KafkaEventConsumer.consume(...)`.
4. Annotate listener with `@ConditionalOnProperty(tokenrealty.kafka.enabled=true)`.
5. One service call per listener — no business logic in the listener body.
6. No `@Transactional` on listener when it triggers HTTP or blockchain; TX on service only.

## Shared libraries

Install before service builds:

```bash
./token-realty-app/mvnw -pl tokenrealty-events,tokenrealty-outbox,tokenrealty-kafka install -DskipTests
```

| Module | Use |
|--------|-----|
| `tokenrealty-events` | `EventEnvelope`, `KafkaJsonEvent` |
| `tokenrealty-outbox` | `OutboxWriter`, `OutboxPayload`, `OutboxRelay` |
| `tokenrealty-kafka` | `KafkaEventConsumer`, `ProcessedEventClaimService` |

## Local dev flags

| Flag | Effect |
|------|--------|
| `KAFKA_ENABLED=false` (default in tests) | OutboxWriter no-ops; listeners disabled |
| `KAFKA_ENABLED=true` | Relay worker + consumers active |
| `docker compose --profile kafka up` | Local broker (see repo root / token-realty-app) |

## Services with Kafka today

| Service | Publishes | Consumes |
|---------|-----------|----------|
| Property Registry | `flat.tokenized` | — |
| Marketplace | `listing.created`, `order.matched`, `trade.settled` | `flat.tokenized`, `payment.confirmed`, `transfer.completed` |
| Payment | `payment.confirmed`, `rent.collected` | `order.matched`, `dividend.distributed` |
| Token Issuance | `transfer.completed`, `dividend.distributed` | `payment.confirmed`, `rent.collected`, `kyc-approved`, `kyc-revoked` |
| Compliance | `kyc-approved`, `kyc-revoked` | — |
| Document | `document.uploaded` | — |
| Notification | — | all notification-worthy topics |
| Rental | (via Payment rent.collected) | — |
