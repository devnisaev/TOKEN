---
name: java-code-review
description: Review TokenRealty Java changes for hexagonal leaks, Kafka claim site, Clock, exception helpers, and RWA/outbox invariants. Use when reviewing a PR, a diff, or when the user asks what is wrong with the Java change.
---

# Java code review (TokenRealty)

Review the diff against `docs/rules/README.md` and the service slice. Do not restyle unrelated files.

## Checklist

- [ ] `application/` does not import `adapter.in.web.dto` or JPA/Kafka/RestClient types
- [ ] 4+ field mutations take a command; HTTP/Kafka map at the adapter
- [ ] Kafka: `Command.from(event)` + `*KafkaEventTypes`; claim where the slice says (consumer vs use case)
- [ ] No `Instant.now()` on persist/claim/dispatch — `Clock` (readiness / ops peeks may stay)
- [ ] `raiseException` / `exception` helpers — no `fail()` factory, no `catch (BusinessException ex) { throw ex; }`
- [ ] Exception HTTP map: validation 400, not_found 404, `*_unavailable` 502 unless the slice says otherwise
- [ ] Same-Tx outbox enqueue on domain writes (`event-zero-loss.mdc`)
- [ ] No private keys, mnemonics, or sensitive user data in logs, events, or non-secure services
- [ ] No fallback approve / invented tenant or currency (`no-fallbacks.mdc`)
- [ ] Lists use `PageRequest.of` at the adapter (`pagination.mdc`)
- [ ] Lombok only where `lombok.mdc` allows — no `@Data` on domain aggregates
- [ ] Tests cover claim skip, command mapping, and exception status — not only the happy path
- [ ] Slice + README + `.mdc` updated if the boundary changed

## Feedback format

- **Must fix** — invariant or layer leak
- **Should fix** — architectural boundary drift
- **Note** — leave-alone that looks like a miss but is documented

Do not demand splitting a cohesive engine or adding ingest-support for different payload shapes.

---

## TokenRealty doc map (checklist → rules)

| Checklist item | TokenRealty rule / doc |
|----------------|-------------------------|
| Service slice | [docs/rules/spring-java-services.md](../../../docs/rules/spring-java-services.md), [java-architect](../java-architect/SKILL.md) |
| Docs index | [docs/README.md](../../../docs/README.md) |
| Layered vs hexagonal | Most services use `controller → service → repository` — full `application/` package exists only in future hexagonal splits |
| Kafka claim + `Command.from` | [.cursor/rules/kafka-messaging.mdc](../../rules/kafka-messaging.mdc), [KafkaEventConsumer](tokenrealty-kafka) |
| Outbox same TX | [business-rules.mdc](../../rules/business-rules.mdc), [kafka-messaging.mdc](../../rules/kafka-messaging.mdc) |
| `event-zero-loss.mdc` | Same as outbox rules above |
| Exception helpers | [.cursor/rules/business-exception.mdc](../../rules/business-exception.mdc) — `ValidationException`, `raiseValidation`, not Titan `BusinessException` |
| Exception HTTP map | TokenRealty: `ValidationException` → **422** (`/errors/business-rule`); `MethodArgumentNotValidException` → **400**; `ResourceNotFoundException` → **404** |
| RestClient errors | [.cursor/rules/rest-client-errors.mdc](../../rules/rest-client-errors.mdc) |
| Pagination | [.cursor/rules/pagination.mdc](../../rules/pagination.mdc) |
| Lombok | [.cursor/rules/lombok.mdc](../../rules/lombok.mdc) |
| DTOs / records | [.cursor/rules/java-dtos.mdc](../../rules/java-dtos.mdc) |
| `no-fallbacks.mdc` | No invented KYC pass, fake wallet approval, or default investor IDs — fail with `ValidationException` |
| RWA / KYC / secrets | [rwa-legal-compliance](../rwa-legal-compliance/SKILL.md), [BUSINESS_RULES.md](../../../docs/BUSINESS_RULES.md) |
| Payment / escrow | [payment-ledger.mdc](../../rules/payment-ledger.mdc) |
| Blockchain | [web3j-blockchain-integration](../web3j-blockchain-integration/SKILL.md) — receipt before success, no keys in code |

## TokenRealty-specific review notes

**Layered services (default):** `service/` importing `client/*`, `kafka/*`, and `repository` is **expected** — flag only if business logic appears in `controller/` or `kafka/in/*Listener` beyond `Command.from` + one service call.

**Kafka idempotency:** Consumers must use `KafkaEventConsumer.consume(...)` + `processed_events` — verify duplicate `eventId` does not double-apply.

**Transactions:** `@Transactional` on service write methods; listeners must not hold TX during HTTP or `waitForReceipt`.

**Compliance order:** Marketplace/Issuance changes must not skip KYC before match or transfer ([investment-limits.mdc](../../rules/investment-limits.mdc)).

**Clock:** Many services still use `Instant.now()` directly — **Should fix** on new persist/claim/outbox paths; do not block entire PR for legacy lines unless touched.

**Tests:** `./mvnw test` in affected service module; shared libs require `token-realty-app/mvnw install` first.

## Leave-alones (TokenRealty)

- Cohesive `OrderService`, `PaymentService`, `TokenIssuanceService`, `DividendService` — do not request split for purity
- Simulated `0xSIMULATED_*` tx hashes when `PAYMENT_BLOCKCHAIN_ENABLED=false` or dev auto-confirm
- Manual ABI encoding in `BlockchainConnector` until Web3j wrappers land
- Hibernate `ddl-auto: update` (Liquibase backlog — note, not must-fix unless migration added)
