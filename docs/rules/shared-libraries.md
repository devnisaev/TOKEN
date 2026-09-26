# Shared Libraries — TokenRealty

Cross-service Maven modules under the repo root. Install before building services:

```bash
./token-realty-app/mvnw -pl tokenrealty-security,tokenrealty-web,tokenrealty-jpa,tokenrealty-kafka,tokenrealty-events,tokenrealty-outbox install
```

---

## Module overview

| Module | Package | Purpose |
|--------|---------|---------|
| `tokenrealty-security` | `com.tokenrealty.security` | JWT validation filter, service account token provider |
| `tokenrealty-web` | `com.tokenrealty.web` | Shared exceptions, RFC 7807 `TokenRealtyExceptionHandler` |
| `tokenrealty-jpa` | `com.tokenrealty.jpa` | `BaseEntity`, `@EnableJpaAuditing` auto-config |
| `tokenrealty-events` | `com.tokenrealty.events` | `EventEnvelope`, `KafkaJsonEvent` parse helper |
| `tokenrealty-kafka` | `com.tokenrealty.kafka` | `KafkaEventConsumer`, `ProcessedEventClaimService` |
| `tokenrealty-outbox` | `com.tokenrealty.outbox` | `OutboxWriter`, `OutboxPayload`, `OutboxRelay`, `OutboxStatus` |

All modules use Spring Boot 4 auto-configuration (`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`). Services add the dependency — no copy-paste setup classes.

---

## tokenrealty-web

**Dependency:** any service with REST APIs.

**Do not copy** per-service `GlobalExceptionHandler` or standard exception classes.

| Class | HTTP | ProblemDetail type |
|-------|------|-------------------|
| `ResourceNotFoundException` | 404 | `/errors/not-found` |
| `ConflictException` | 409 | `/errors/conflict` |
| `IdempotencyConflictException` | 409 | `/errors/conflict` |
| `ValidationException` | 422 | `/errors/business-rule` |
| `InsufficientFundsException` | 422 | `/errors/business-rule` |

`TokenRealtyExceptionHandler` also maps bean validation (400), data integrity (409), malformed JSON (400), and type mismatch (400).

**Keep per-service:** domain-only exceptions (e.g. `ComplianceException` in Issuance). Map them in a local `@RestControllerAdvice` if needed, or extend the shared handler later.

```java
import com.tokenrealty.web.exception.ValidationException;
import com.tokenrealty.web.exception.ResourceNotFoundException;

throw new ResourceNotFoundException("Lease", leaseId);
```

---

## tokenrealty-jpa

**Dependency:** any service with JPA entities.

**Do not copy** `BaseEntity` or `@EnableJpaAuditing` config classes.

```java
import com.tokenrealty.jpa.entity.BaseEntity;

@Entity
@Table(name = "leases")
public class Lease extends BaseEntity {
    // id, createdAt, updatedAt, version inherited
}
```

`TokenRealtyJpaAutoConfiguration` enables auditing globally. Remove duplicate `@EnableJpaAuditing` from service `AppConfig` classes.

---

## tokenrealty-kafka

**Dependency:** services that consume Kafka (`tokenrealty.kafka.enabled=true`).

**Do not copy** `ProcessedEvent`, `ProcessedEventService`, or `*KafkaIngestSupport`.

Shared components:

| Class | Role |
|-------|------|
| `ProcessedEvent` | JPA entity (`processed_events` table) — registered via `@AutoConfigurationPackage` |
| `ProcessedEventClaimService` | `tryClaim(eventId, eventType)` — idempotency before handler |
| `KafkaEventConsumer` | Wraps `KafkaJsonEvent.consume` + claim |

```java
@Component
@RequiredArgsConstructor
public class OrderMatchedListener {

    private final KafkaEventConsumer eventConsumer;
    private final OrderEscrowService orderEscrowService;

    @KafkaListener(topics = "${tokenrealty.kafka.topic.order-matched}")
    public void onOrderMatched(String message) {
        eventConsumer.consume(message, PaymentKafkaEventTypes.ORDER_MATCHED,
                "Order matched reconciliation failed",
                event -> orderEscrowService.ensureEscrowLinked(OrderMatchedCommand.from(event)));
    }
}
```

Each consuming service still owns `kafka/in/*Listener` and `kafka/command/*Command` — only the ingest shell is shared.

---

## tokenrealty-outbox

**Dependency:** services that publish domain events.

Per-service (not shared):

* `OutboxEvent` entity + `OutboxEventRepository` (local `outbox_events` table)
* `OutboxWriter` subclass (calls `persistOutboxEvent`)
* `Outbox{Event}Publisher` (domain-specific event types and partition keys)
* Thin `OutboxRelayWorker` (polls repo, delegates to shared relay)

Shared relay logic:

```java
@Scheduled(fixedDelayString = "${tokenrealty.kafka.relay.poll-ms:1000}")
public void relayPending() {
    var pending = repository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
    OutboxRelay.relay(pending, kafkaTemplate, publishTimeoutMs, maxRetries, repository::save, log);
}
```

`OutboxEvent` implements `OutboxRelayTarget` and uses shared `OutboxStatus` enum.

---

## tokenrealty-security — RestClient

**Do not copy** authenticated `RestClient` builder boilerplate.

```java
import com.tokenrealty.security.client.ServiceRestClientBuilder;

@Bean("paymentRestClient")
RestClient paymentRestClient(
        @Value("${services.payment.url}") String baseUrl,
        ObjectProvider<ServiceTokenProvider> serviceTokenProvider) {
    return ServiceRestClientBuilder.build(baseUrl, serviceTokenProvider);
}
```

Domain clients (`PaymentClient`, `TokenIssuanceClient`, …) stay per-service — only the bean factory is shared. See [rest-client-errors.md](rest-client-errors.md).

---

## New service dependency checklist

| Need | Add dependency |
|------|----------------|
| REST API + ProblemDetail | `tokenrealty-web` |
| JPA entities | `tokenrealty-jpa` |
| JWT / service tokens | `tokenrealty-security` |
| Kafka consumer | `tokenrealty-kafka` + `tokenrealty-events` |
| Kafka producer (outbox) | `tokenrealty-outbox` + `tokenrealty-events` |

---

## What stays per-service

| Keep local | Why |
|------------|-----|
| `*Listener` classes | Domain reactions differ |
| `Outbox*Publisher` | Event types and partition keys are domain |
| `*Client` method bodies | URIs, DTOs, error semantics vary |
| Kafka topic config | `application.yml` per service |
| Business rules | No shared inheritance for domain logic |
