---
name: java-implementation
description: Write or change TokenRealty production Java — hexagonal commands, views, Kafka ingest, Clock, exception maps, and docs. Use when implementing, adding endpoints, aligning a service, or refactoring Java without changing behavior.
---

# Java implementation (TokenRealty)

Follow `spring-java-services.mdc` and the service slice in `hexagonal-boundaries.md`. Refactors use the same path with **no behavior change**.

## Before coding

- Read the service’s hexagonal slice and `application-commands.mdc`.
- If the user asked for a plan first, finish `java-architect` and wait for “start”.
- Do not commit unless asked.

## Write path

| Boundary | Do | Do not |
|----------|----|--------|
| HTTP | `toCommand()` at adapter; one use-case call; `Response.from(view)` | Unpack 4+ fields; return `Map`; import web DTOs in `application/` |
| Application | Command records; ports; `raiseException` | Positional 4+ args; `*V2` services; `Instant.now()` on persist/claim |
| Kafka | `KafkaJsonEvent.consume` → `Command.from` → claim → one service call | `readTree` in the listener; claim on the use case when the slice says consumer |
| Time | `Clock` on `*Config` for write-path "now" | `Clock` on `*KafkaConfig`; inventing `Clock` for ops-only / readiness |
| REST out | `*Port` + `*RestErrors` / existing helper | `RestClient` in application; `fail()` factory |
| Docs | Update slice + README + matching `.mdc` when the boundary changes | New markdown files for one service |

## Refactor leave-alones

Do not split cohesive engines. Do not type ops `details` maps. Do not paginate documented unpaged peeks.

## After coding

Parent POM is **JDK 21**. Use whatever `JAVA_HOME` the machine already uses — never commit a personal path.

```bash
mvn -pl services/<service-name> test
```

If `java -version` is not 21, point `JAVA_HOME` at a JDK 21 install (macOS: `/usr/libexec/java_home -v 21`; Windows: `$env:JAVA_HOME` from that machine’s JDK 21), then retry. Mockito inline mock-maker needs the command run outside the Cursor sandbox (`required_permissions: ["all"]`). Fix failures before declaring done.

---

## TokenRealty doc map (write path → repo)

| Referenced in skill | TokenRealty equivalent |
|---------------------|-------------------------|
| `spring-java-services.mdc` | [.cursor/rules/spring-java-services.mdc](../../rules/spring-java-services.mdc), [docs/rules/spring-java-services.md](../../../docs/rules/spring-java-services.md) |
| `hexagonal-boundaries.md` | Same spring-java-services doc § layout — full hexagonal slices are **future**; today use layered `controller → service → repository` |
| `application-commands.mdc` | [.cursor/rules/java-dtos.mdc](../../rules/java-dtos.mdc) — records for request/command/Kafka payloads |
| `raiseException` | [business-exception.mdc](../../rules/business-exception.mdc) — `ValidationException`, `ResourceNotFoundException`, `ConflictException` |
| `KafkaJsonEvent.consume` | [tokenrealty-events](../../../tokenrealty-events/) + [KafkaEventConsumer](../../../tokenrealty-kafka/) via [kafka-messaging.mdc](../../rules/kafka-messaging.mdc) |
| Outbox on domain write | [business-rules.mdc](../../rules/business-rules.mdc), [kafka-messaging.mdc](../../rules/kafka-messaging.mdc) |
| REST out | `client/*Client` + [rest-client-errors.mdc](../../rules/rest-client-errors.mdc) — not separate `*Port` types yet |
| Pagination | [pagination.mdc](../../rules/pagination.mdc) — `PageRequest.of` in controller |
| Lombok | [lombok.mdc](../../rules/lombok.mdc) |
| Tier-1 invariants | [BUSINESS_RULES.md](../../../docs/BUSINESS_RULES.md) |

## TokenRealty layered write path (current default)

Most services today — apply the **spirit** of the table above without forcing a full hexagonal package split:

```text
controller/     ← @Valid DTO → service method (or future toCommand())
service/        ← @Transactional writes; ValidationException on rule break
repository/     ← JPA
kafka/in/       ← KafkaEventConsumer → *Command.from(event) → service
kafka/outbox/   ← OutboxWriter in same TX as domain save
client/         ← ServiceRestClientBuilder inter-service calls
```

**HTTP:** Controllers validate and delegate — map request record fields to service args or a command record when 4+ fields mutate.

**Kafka:** Listeners stay thin — parse via shared consumer helper, map to `kafka/command/*Command`, one service call.

**Exceptions:** Throw typed exceptions from service layer; `TokenRealtyExceptionHandler` maps to RFC 7807 (`ValidationException` → **422**).

**Money / escrow / KYC:** Read [payment-ledger.mdc](../../rules/payment-ledger.mdc) and [investment-limits.mdc](../../rules/investment-limits.mdc) before touching Payment or Marketplace buy paths.

## TokenRealty leave-alones

- Do not refactor `PaymentService`, `OrderService`, `TokenIssuanceService`, or `DividendService` into multiple use cases unless explicitly requested.
- Do not add `application/` package trees to mature layered services in a behavior-preserving refactor.
- Simulated `0xSIMULATED_*` tx hashes and manual ABI encoding in `BlockchainConnector` — leave as-is unless the task is on-chain work ([web3j-blockchain-integration](../web3j-blockchain-integration/SKILL.md)).
- Hibernate `ddl-auto: update` — note only; Liquibase is backlog.

## After coding (TokenRealty commands)

```bash
# Shared libs (when touching tokenrealty-* modules or ClassNotFound in tests)
./token-realty-app/mvnw -pl tokenrealty-security,tokenrealty-web,tokenrealty-jpa,tokenrealty-kafka,tokenrealty-events,tokenrealty-outbox install

# Single service — from service directory
cd marketplace-service && ./mvnw test

# Registry (Property Registry lives here)
cd token-realty-app && ./mvnw test
```

## Related skills

- [java-architect](../java-architect/SKILL.md) — plan before large boundary changes
- [java-code-review](../java-code-review/SKILL.md) — self-check before PR
- [java-debugging](../java-debugging/SKILL.md) — when tests fail after implementation
- [kafka-event-architect](../kafka-event-architect/SKILL.md) — new topics/listeners/outbox payloads
- [rwa-legal-compliance](../rwa-legal-compliance/SKILL.md) — Compliance, Registry, Document changes
