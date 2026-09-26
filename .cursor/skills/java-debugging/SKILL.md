---
name: java-debugging
description: Diagnose TokenRealty Java test and runtime failures — Surefire, Mockito inline, Kafka parse logs, BusinessException maps. Use when tests fail, stack traces appear, NPE, ClassNotFound, or the user asks to fix a red build.
---

# Java debugging (TokenRealty)

Reproduce first. Do not “fix” by weakening assertions or catching `Exception`.

## Reproduce

```bash
mvn -pl services/<service-name> test -Dtest=<ClassName>
```

Need JDK 21. Do not paste a personal `JAVA_HOME` into the skill or the command you commit. Set it on the machine: macOS `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`; Windows `$env:JAVA_HOME` from that PC’s JDK 21.

Sandbox: Mockito inline mock-maker often fails without `required_permissions: ["all"]`. Retry with `all` before changing code.

## Read the failure

| Signal | Likely cause |
|--------|----------------|
| `UnnecessaryStubbing` / unused `eq` | Tighten stubs or drop unused imports |
| `Wanted but not invoked` after Kafka change | Claim moved to the consumer — assert on the listener test, not the use case |
| `Bean of type Clock` | Add `Clock` to `*Config`, not `*KafkaConfig` |
| `HttpRuntimeConfig` / `localhost:0` | `@Primary RuntimeConfig` stub + disable config HTTP client |
| `JsonParseException` + ERROR from `KafkaJsonEvent` | Invalid-JSON test — pass if `assertThrows` expects it |
| `BusinessException` code mismatch | Handler / `BusinessErrorCodes` vs leftover literal |

## Then

1. Fix the production boundary (command, claim site, Clock) — not the test message.
2. Re-run the same `-Dtest=` plus the module `test`.
3. If the slice docs are now wrong, update [hexagonal-boundaries.md](../../../docs/rules/hexagonal-boundaries.md).

---

## TokenRealty doc map (reproduce → run)

| Titan / generic | TokenRealty equivalent |
|-----------------|-------------------------|
| `mvn -pl services/<service-name> test` | `cd <service-dir> && ./mvnw test -Dtest=<ClassName>` — e.g. `marketplace-service/`, `payment-service/`, `token-issuance-service/`, `token-realty-app/` (Registry) |
| Root / shared libs first | `./token-realty-app/mvnw -pl tokenrealty-security,tokenrealty-web,tokenrealty-jpa,tokenrealty-kafka,tokenrealty-events,tokenrealty-outbox install` before service tests if `ClassNotFound` on `com.tokenrealty.*` libs |
| Full module suite | `./mvnw test` inside the service folder |
| Exception handler | [TokenRealtyExceptionHandler](../../../tokenrealty-web/src/main/java/com/tokenrealty/web/exception/TokenRealtyExceptionHandler.java) — not Titan `BusinessException` |
| Kafka parse / claim | [KafkaEventConsumer](../../../tokenrealty-kafka/) + [kafka-messaging.mdc](../../rules/kafka-messaging.mdc) |
| Slice docs | [docs/rules/spring-java-services.md](../../../docs/rules/spring-java-services.md) — not `hexagonal-boundaries.md` |

## TokenRealty failure signals

| Signal | Likely cause |
|--------|----------------|
| `ClassNotFoundException: com.tokenrealty.web` (or kafka/events/security) | Install shared libs (`install` command above) |
| `Could not find artifact com.tokenrealty:tokenrealty-*` | Same — run install from repo root via `token-realty-app/mvnw` |
| `ValidationException` / status **422** mismatch | Assert `HttpStatus.UNPROCESSABLE_ENTITY` + `/errors/business-rule` — not 400 |
| `MethodArgumentNotValidException` / status **400** | Bean validation on `@Valid` DTO — `/errors/validation` |
| `ResourceNotFoundException` / **404** | `/errors/not-found` |
| `ConflictException` / idempotency replay | **409** `/errors/conflict` |
| Listener never calls service | Missing `@KafkaListener` profile, Kafka disabled in test, or event type string mismatch vs [EVENTS.md](../../../docs/EVENTS.md) |
| Duplicate side effect on replay | `KafkaEventConsumer` / `processed_events` claim not wired — check listener uses shared consumer helper |
| Outbox row missing after write | `@Transactional` rollback, or publish outside same TX as domain save |
| `Connection refused` PostgreSQL | Service started without DB; integration tests may need Testcontainers or `@DataJpaTest` slice |
| Blockchain / Web3j NPE in Payment/Issuance | `PAYMENT_BLOCKCHAIN_ENABLED` / RPC URL unset — use `local` profile or mock `BlockchainConnector` in unit tests |
| RestClient **502** in logs | Downstream service down or wrong base URL in `application-local.yml` — not a handler bug |

## TokenRealty-specific notes

**Layered tests:** Most failures are in `*ServiceTest` with `@ExtendWith(MockitoExtension.class)` — fix service boundary and command mapping, not controller unless `@WebMvcTest` is red.

**Registry vs services:** Property Registry lives in `token-realty-app/` (`com.tokenrealty.registry`), not a top-level `property-registry-service/` folder.

**Do not:** weaken `assertThat(...).isEqualTo(422)` to pass; stub `Clock` with a fixed instant instead of deleting time assertions; catch `Exception` in production to green a test.

**Docs update:** If claim site or exception map changed, update [business-exception.mdc](../../rules/business-exception.mdc) and the service README — not only the test.

## Related skills

- [java-code-review](../java-code-review/SKILL.md) — invariant checklist after fix
- [java-architect](../java-architect/SKILL.md) — when the failure reveals a boundary design gap
- [kafka-event-architect](../kafka-event-architect/SKILL.md) — listener / outbox wiring
- [web3j-blockchain-integration](../web3j-blockchain-integration/SKILL.md) — on-chain test failures
