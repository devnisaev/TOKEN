---
name: java-testing
description: Write TokenRealty JUnit 5 / Mockito tests for Spring services — Clock stubs, RuntimeConfig, Kafka consumers, and command/view boundaries. Use when adding tests, fixing test compile, coverage, or when mentioning JUnit, Mockito, or Surefire.
---

# Java testing (TokenRealty)

Match the nearest `*Test.java` in the same package. Do not add Spring Boot test slices unless the existing test already uses them.

## Conventions

- JUnit 5 + Mockito. Constructor injection — new the service with mocks.
- `Clock`: `when(clock.instant()).thenReturn(...)`. Use `lenient()` when some tests never hit `clock.instant()`.
- `RuntimeConfig`: stub `getBoolean` / `getDecimal` keys the code reads. Do not construct `HttpRuntimeConfig` against `localhost:0`.
- Kafka consumer tests: `KafkaJsonEvent` JSON envelope; assert `tryClaim` skip does **not** call the use case; use `*KafkaEventTypes` constants.
- HTTP controller tests: DTO `toCommand()` / `from(view)` — do not unpack fields in the test if the controller does not.
- Exception map: table-drive `GlobalExceptionHandler` (400 / 404 / 502 / 422). Canonical codes via `BusinessErrorCodes` when the production switch uses them.

## Forbidden in tests

- `@SneakyThrows` to hide checked exceptions
- Asserting web DTO types from an application-service unit test
- Copying `Instant.now()` into production to “make the test easier” — inject `Clock`

## Run

JDK 21 (parent POM). Do not hardcode a machine `JAVA_HOME`.

```bash
mvn -pl services/<service-name> test
```

If the default `java` is not 21, set `JAVA_HOME` locally (macOS: `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`; Windows: `$env:JAVA_HOME` from that PC’s JDK 21). Use `required_permissions: ["all"]` in Cursor so Mockito inline can attach. Expected ERROR logs from invalid-JSON consumer tests are not failures if Surefire exits 0.

---

## TokenRealty doc map (conventions → repo)

| Referenced in skill | TokenRealty equivalent |
|---------------------|-------------------------|
| `GlobalExceptionHandler` | [TokenRealtyExceptionHandler](../../../tokenrealty-web/src/main/java/com/tokenrealty/web/exception/TokenRealtyExceptionHandler.java) |
| `BusinessErrorCodes` | **N/A** — assert ProblemDetail `type` URI (`/errors/business-rule`, `/errors/not-found`, etc.) and HTTP status |
| `RuntimeConfig` / `HttpRuntimeConfig` | **N/A** for most services — use `@Value` / profile properties or mock config beans if introduced |
| Service unit tests | `@ExtendWith(MockitoExtension.class)` + `@Mock` / `@InjectMocks` — see `OrderServiceTest`, `PaymentServiceTest` |
| Controller tests | `@WebMvcTest` + `@Import(TokenRealtyExceptionHandler.class, …)` — see `BuildingControllerTest` |
| Kafka envelope | [EventEnvelope](../../../tokenrealty-events/) + [KafkaJsonEvent](../../../tokenrealty-events/) via [EVENTS.md](../../../docs/EVENTS.md) |
| Claim skip | Mock `ProcessedEventClaimService.tryClaim` returning `false` — service must not run |
| Shared libs in tests | Install `tokenrealty-*` modules first if compile fails — see [AGENTS.md](../../../AGENTS.md) |

## TokenRealty exception map (table-drive)

| Exception | HTTP | ProblemDetail type |
|-----------|------|-------------------|
| `MethodArgumentNotValidException` | 400 | `/errors/validation` |
| `ValidationException`, `InsufficientFundsException` | 422 | `/errors/business-rule` |
| `ResourceNotFoundException`, `EntityNotFoundException` | 404 | `/errors/not-found` or `/errors/entity-not-found` |
| `ConflictException`, `IdempotencyConflictException` | 409 | `/errors/conflict` |
| Downstream RestClient failure | 502 | per [rest-client-errors.mdc](../../rules/rest-client-errors.mdc) |

Use `mockMvc.perform(...).andExpect(status().isUnprocessableEntity())` for business-rule rejections — not 400.

## TokenRealty test patterns

**Service unit test (default):**

```java
@ExtendWith(MockitoExtension.class)
class FooServiceTest {
    @Mock FooRepository repository;
    @Mock SomeClient client;
    @InjectMocks FooService service;

    @Test
    void rejectsWhenRuleBroken() {
        assertThatThrownBy(() -> service.doThing(...))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("...");
    }
}
```

**Controller test (when slice already has one):** `@WebMvcTest`, `@MockitoBean` for service, import `TokenRealtyExceptionHandler`, use `@WithMockUser` / `csrf()` where security applies.

**Kafka listener test (when adding):** Build minimal JSON matching `EventEnvelope`; verify duplicate `eventId` skips service via claim mock; use topic/event-type constants from the service’s `*KafkaEventTypes` or [EVENTS.md](../../../docs/EVENTS.md).

**Payment / escrow:** Cover idempotency replay and conflict paths — not only happy path ([payment-ledger.mdc](../../rules/payment-ledger.mdc)).

**Blockchain:** Mock `BlockchainConnector` in unit tests; do not require Hardhat RPC unless `@SpringBootTest` integration is explicitly requested.

## TokenRealty leave-alones

- No `@SpringBootTest` + Testcontainers unless the module already uses that pattern.
- `@DataJpaTest` only when testing repository queries in isolation — most tests mock repositories.
- AssertJ over JUnit assertions — matches existing tests.
- Do not add Kafka listener tests that require a running broker in CI unless docker-compose is wired.

## Run (TokenRealty commands)

```bash
# Shared libs first (ClassNotFound on com.tokenrealty.*)
./token-realty-app/mvnw -pl tokenrealty-security,tokenrealty-web,tokenrealty-jpa,tokenrealty-kafka,tokenrealty-events,tokenrealty-outbox install

# Single test class
cd payment-service && ./mvnw test -Dtest=PaymentServiceTest

# Full module
cd marketplace-service && ./mvnw test
```

## Related skills

- [java-debugging](../java-debugging/SKILL.md) — when tests fail after writing them
- [java-implementation](../java-implementation/SKILL.md) — production boundary the test should mirror
- [java-code-review](../java-code-review/SKILL.md) — claim skip, command mapping, exception status checklist
- [kafka-event-architect](../kafka-event-architect/SKILL.md) — envelope shape for listener tests
