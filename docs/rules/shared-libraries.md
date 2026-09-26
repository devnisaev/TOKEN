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
| `tokenrealty-web` | `com.tokenrealty.web` | Exceptions, RFC 7807 handler, observability, outbound REST helpers |
| `tokenrealty-jpa` | `com.tokenrealty.jpa` | `BaseEntity`, `@EnableJpaAuditing` auto-config |
| `tokenrealty-events` | `com.tokenrealty.events` | `EventEnvelope`, `KafkaJsonEvent` parse helper |
| `tokenrealty-kafka` | `com.tokenrealty.kafka` | `KafkaEventConsumer`, `ProcessedEventClaimService` |
| `tokenrealty-outbox` | `com.tokenrealty.outbox` | `OutboxWriter`, `OutboxPayload`, `OutboxRelay`, `OutboxStatus` |

All modules use Spring Boot 4 auto-configuration (`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`). Services add the dependency — no copy-paste setup classes.

---

## tokenrealty-web

**Dependency:** any service with REST APIs or outbound HTTP calls to other TokenRealty services.

**Do not copy** per-service `GlobalExceptionHandler`, standard exception classes, trace filters, or RestClient try/catch blocks.

### Exceptions & RFC 7807

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

### Observability (`com.tokenrealty.web.observability`)

Auto-configured when `tokenrealty-web` is on the classpath. See [observability.md](observability.md).

| Class / resource | Role |
|------------------|------|
| `TraceIdFilter` | Reads or generates `X-Trace-Id`, puts `traceId` in MDC, echoes header on response |
| `ObservabilityEnvironmentPostProcessor` | Enables JSON logback + Prometheus registry for `json-log` / `prod` profiles |
| `application-otel.yml` | Optional OTLP export — activate with profile `otel` or `OTEL_ENABLED=true` |
| Micrometer OTel bridge | Transitive via `tokenrealty-web` — exports spans to Jaeger when enabled |

Constants live in `RestHeaders.TRACE_ID` and `RestHeaders.TRACE_ID_MDC`.

```bash
OTEL_ENABLED=true OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318 \
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=local,otel
```

### Outbound REST (`com.tokenrealty.web.rest`)

Shared inter-service HTTP stack. Full guide: [rest-client-errors.md](rest-client-errors.md).

| Class | Role |
|-------|------|
| `RestHeaders` | `X-Trace-Id`, MDC key, `Idempotency-Key` |
| `RestClientOperations` | Raw GET/POST/PATCH/PUT — no error mapping |
| `DownstreamServices` | `ServiceSpec` constants (`PAYMENT`, `COMPLIANCE`, …) for error messages |
| `DownstreamClientErrors` | Maps 5xx/timeout → `ValidationException`; 404 → `ResourceNotFoundException` |
| `DownstreamRestClientSupport` | Base class for `*Client` adapters — **required**; wraps `RestClientOperations` + error mapping |

```java
@Component
public class PaymentClient extends DownstreamRestClientSupport {

    public PaymentClient(@Qualifier("paymentRestClient") RestClient restClient) {
        super(restClient);
    }

    public InitiatePaymentResponse initiateTokenPurchase(UUID orderId, InitiatePaymentRequest body) {
        return post("/v1/payments", body, InitiatePaymentResponse.class,
                DownstreamServices.PAYMENT,
                RestClientOperations.idempotencyKey("marketplace-order-" + orderId));
    }
}
```

Tests: `tokenrealty-web/src/test/.../DownstreamClientErrorsTest.java`.

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

## tokenrealty-security — RestClient builder

**Do not copy** authenticated `RestClient` builder boilerplate (Bearer token, timeout, trace propagation).

`ServiceRestClientBuilder` (`com.tokenrealty.security.client`):

| Overload | Behavior |
|----------|----------|
| `build(baseUrl, serviceTokenProvider)` | Default 10s connect + read timeout |
| `build(baseUrl, readTimeout, serviceTokenProvider)` | Explicit timeout per downstream |

Request interceptor attaches:

1. `Authorization: Bearer {serviceToken}` when `ServiceTokenProvider` is present
2. `X-Trace-Id` from MDC key `traceId` (must match `RestHeaders.TRACE_ID_MDC`)

```java
import com.tokenrealty.security.client.ServiceRestClientBuilder;
import java.time.Duration;

@Bean("paymentRestClient")
RestClient paymentRestClient(
        @Value("${services.payment.url}") String baseUrl,
        ObjectProvider<ServiceTokenProvider> serviceTokenProvider) {
    return ServiceRestClientBuilder.build(
            baseUrl, Duration.ofSeconds(10), serviceTokenProvider);
}
```

Domain clients (`PaymentClient`, `TokenIssuanceClient`, …) stay per-service — **must** extend `DownstreamRestClientSupport` and use `RestClientOperations` (via base `get`/`post`/…); never call raw `RestClient` in adapter methods. See [rest-client-errors.md](rest-client-errors.md).

---

## New service dependency checklist

| Need | Add dependency |
|------|----------------|
| REST API + ProblemDetail | `tokenrealty-web` |
| Outbound calls to other services | `tokenrealty-web` + `tokenrealty-security` |
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
| `*Client` method bodies | URIs, DTOs, idempotency keys — use shared error mapping base |
| Kafka topic config | `application.yml` per service |
| Business rules | No shared inheritance for domain logic |
