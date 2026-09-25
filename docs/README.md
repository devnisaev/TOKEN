# TokenRealty — Platform Documentation

Central documentation for the TokenRealty real-estate tokenization platform.

## Documents

| Document | Description |
|----------|-------------|
| [**PLATFORM-SPEC.md**](PLATFORM-SPEC.md) | Master implementation spec & TODO backlog |
| [**PLATFORM-SPEC.html**](PLATFORM-SPEC.html) | HTML version (browser-friendly) |
| [**EVENTS.md**](EVENTS.md) | Kafka topic catalog and payload schemas |
| [**../README.md**](../README.md) | Root platform overview & startup order |
| [**../AGENTS.md**](../AGENTS.md) | Cursor agent instructions |

## Coding standards (human-readable)

| Guide | Cursor rule |
|-------|-------------|
| [rules/spring-java-services.md](rules/spring-java-services.md) | [spring-java-services.mdc](../.cursor/rules/spring-java-services.mdc) |
| [rules/lombok.md](rules/lombok.md) | [lombok.mdc](../.cursor/rules/lombok.mdc) |
| [rules/java-dtos.md](rules/java-dtos.md) | [java-dtos.mdc](../.cursor/rules/java-dtos.mdc) |
| [rules/kafka-messaging.md](rules/kafka-messaging.md) | [kafka-messaging.mdc](../.cursor/rules/kafka-messaging.mdc) |
| [rules/rest-client-errors.md](rules/rest-client-errors.md) | [rest-client-errors.mdc](../.cursor/rules/rest-client-errors.mdc) |
| [rules/payment-ledger.md](rules/payment-ledger.md) | [payment-ledger.mdc](../.cursor/rules/payment-ledger.mdc) |
| [rules/investment-limits.md](rules/investment-limits.md) | [investment-limits.mdc](../.cursor/rules/investment-limits.mdc) |

Additional Cursor rules (no separate human doc yet):

| Rule | Description |
|------|-------------|
| [business-exception.mdc](../.cursor/rules/business-exception.mdc) | ProblemDetail & typed exceptions |
| [pagination.mdc](../.cursor/rules/pagination.mdc) | List endpoint paging defaults |

## Services

| Service | README | Port |
|---------|--------|------|
| Property Registry | [token-realty-app/README.md](../token-realty-app/README.md) | 8081 |
| Token Issuance | [token-issuance-service/README.md](../token-issuance-service/README.md) | 8082 |
| Auth | [auth-service/README.md](../auth-service/README.md) | 8083 |
| Marketplace | [marketplace-service/README.md](../marketplace-service/README.md) | 8084 |
| Payment | [payment-service/README.md](../payment-service/README.md) | 8085 |

## Diagrams (PlantUML)

Render with [PlantUML](https://plantuml.com/), VS Code **PlantUML** extension, or:

```bash
brew install plantuml
plantuml docs/diagrams/*.puml
```

| File | Description |
|------|-------------|
| [01-platform-overview.puml](diagrams/01-platform-overview.puml) | Current services setup |
| [02-target-architecture.puml](diagrams/02-target-architecture.puml) | Full target microservices architecture |
| [03-token-lifecycle.puml](diagrams/03-token-lifecycle.puml) | Token lifecycle (8 steps) |
| [04-sell-flow.puml](diagrams/04-sell-flow.puml) | Primary market buy flow (crypto) |
| [05-rent-flow.puml](diagrams/05-rent-flow.puml) | Rent collection → dividend flow |
| [06-kafka-events.puml](diagrams/06-kafka-events.puml) | Kafka event bus topology |
| [07-build-phases.puml](diagrams/07-build-phases.puml) | Implementation phase timeline |
