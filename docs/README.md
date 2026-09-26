# TokenRealty — Platform Documentation

Central documentation for the TokenRealty real-estate tokenization platform.

## Documents

| Document | Description |
|----------|-------------|
| [**PLATFORM-SPEC.md**](PLATFORM-SPEC.md) | Master implementation spec & TODO backlog |
| [**PLATFORM-SPEC.html**](PLATFORM-SPEC.html) | HTML version (browser-friendly) |
| [**EVENTS.md**](EVENTS.md) | Kafka topic catalog and payload schemas |
| [**schemas/**](schemas/) | JSON Schema contracts for settlement-flow Kafka payloads |
| [rules/service-readmes.md](rules/service-readmes.md) | Service README API table sync checklist |
| [**BUSINESS_RULES.md**](BUSINESS_RULES.md) | Tier-1 platform invariants (money, KYC, outbox, flows) |
| [**hardhat-demo.md**](hardhat-demo.md) | Local on-chain demo: invest → token → dividend |
| Demo infra script | [`../scripts/demo-start.sh`](../scripts/demo-start.sh) — Postgres + Kafka + Jaeger + startup checklist |
| E2E runner | [`../scripts/e2e-run.sh`](../scripts/e2e-run.sh) — wait for gateway + Playwright `test:full` |
| Demo services | [`../scripts/demo-services.sh`](../scripts/demo-services.sh) — background buy-flow stack |
| [**../README.md**](../README.md) | Root platform overview & startup order |
| [**../AGENTS.md**](../AGENTS.md) | Cursor agent instructions |

## Coding standards (human-readable)

| Guide | Cursor rule |
|-------|-------------|
| [rules/spring-java-services.md](rules/spring-java-services.md) | [spring-java-services.mdc](../.cursor/rules/spring-java-services.mdc) |
| [rules/lombok.md](rules/lombok.md) | [lombok.mdc](../.cursor/rules/lombok.mdc) |
| [rules/java-dtos.md](rules/java-dtos.md) | [java-dtos.mdc](../.cursor/rules/java-dtos.mdc) |
| [rules/kafka-messaging.md](rules/kafka-messaging.md) | [kafka-messaging.mdc](../.cursor/rules/kafka-messaging.mdc) |
| [rules/shared-libraries.md](rules/shared-libraries.md) | — (cross-service Maven modules) |
| [rules/rest-client-errors.md](rules/rest-client-errors.md) | [rest-client-errors.mdc](../.cursor/rules/rest-client-errors.mdc) |
| [rules/payment-ledger.md](rules/payment-ledger.md) | [payment-ledger.mdc](../.cursor/rules/payment-ledger.mdc) |
| [rules/investment-limits.md](rules/investment-limits.md) | [investment-limits.mdc](../.cursor/rules/investment-limits.mdc) |
| [rules/business-rules.md](rules/business-rules.md) | [business-rules.mdc](../.cursor/rules/business-rules.mdc) |
| [rules/wallet-service.md](rules/wallet-service.md) | — (custodial wallets, Web3j signing) |
| [rules/blockchain-indexer.md](rules/blockchain-indexer.md) | — (on-chain event poll, reconciliation) |
| [rules/investor-portal.md](rules/investor-portal.md) | [investor-portal.mdc](../.cursor/rules/investor-portal.mdc) |
| [rules/admin-dashboard.md](rules/admin-dashboard.md) | [admin-dashboard.mdc](../.cursor/rules/admin-dashboard.mdc) |
| [rules/tenant-portal.md](rules/tenant-portal.md) | [tenant-portal.mdc](../.cursor/rules/tenant-portal.mdc) |
| [rules/api-gateway-bff.md](rules/api-gateway-bff.md) | — (BFF aggregates on API Gateway) |
| [rules/observability.md](rules/observability.md) | — (traceId, JSON logs, Prometheus) |
| [rules/commit-messages.md](rules/commit-messages.md) | [commit-messages.mdc](../.cursor/rules/commit-messages.mdc) |
| [rules/e2e-testing.md](rules/e2e-testing.md) | — (Playwright smoke + full flow, demo-start) |

Cursor skills (workflows — see [AGENTS.md](../AGENTS.md)):

| Skill | When |
|-------|------|
| [.cursor/skills/java-architect/](../.cursor/skills/java-architect/SKILL.md) | Service structure, plan before implementation |
| [.cursor/skills/java-implementation/](../.cursor/skills/java-implementation/SKILL.md) | Production Java — endpoints, Kafka, refactors |
| [.cursor/skills/java-code-review/](../.cursor/skills/java-code-review/SKILL.md) | Review PRs for layers, Kafka, exceptions |
| [.cursor/skills/java-debugging/](../.cursor/skills/java-debugging/SKILL.md) | Diagnose failing tests and runtime errors |
| [.cursor/skills/java-testing/](../.cursor/skills/java-testing/SKILL.md) | Write JUnit 5 / Mockito tests |
| [.cursor/skills/solidity-smart-contract/](../.cursor/skills/solidity-smart-contract/SKILL.md) | Solidity, Hardhat, contract security |
| [.cursor/skills/kafka-event-architect/](../.cursor/skills/kafka-event-architect/SKILL.md) | Topics, outbox, listeners |
| [.cursor/skills/rwa-legal-compliance/](../.cursor/skills/rwa-legal-compliance/SKILL.md) | KYC, SPV, data room, jurisdiction |
| [.cursor/skills/web3j-blockchain-integration/](../.cursor/skills/web3j-blockchain-integration/SKILL.md) | Web3j, RPC, on-chain transfers |

Additional Cursor rules (no separate human doc yet):

| Rule | Description |
|------|-------------|
| [business-exception.mdc](../.cursor/rules/business-exception.mdc) | ProblemDetail & typed exceptions |
| [pagination.mdc](../.cursor/rules/pagination.mdc) | List endpoint paging defaults |

## Services

| Service | README | Port | Status |
|---------|--------|------|--------|
| API Gateway | [api-gateway/README.md](../api-gateway/README.md) | 8080 | Implemented |
| Property Registry | [token-realty-app/README.md](../token-realty-app/README.md) | 8081 | Implemented |
| Token Issuance | [token-issuance-service/README.md](../token-issuance-service/README.md) | 8082 | Implemented |
| Auth | [auth-service/README.md](../auth-service/README.md) | 8083 | Implemented |
| Marketplace | [marketplace-service/README.md](../marketplace-service/README.md) | 8084 | Implemented |
| Payment | [payment-service/README.md](../payment-service/README.md) | 8085 | Implemented |
| Rental | [rental-service/README.md](../rental-service/README.md) | 8086 | Implemented |
| Compliance | [compliance-service/README.md](../compliance-service/README.md) | 8087 | Implemented |
| Document | [document-service/README.md](../document-service/README.md) | 8088 | Implemented |
| Notification | [notification-service/README.md](../notification-service/README.md) | 8089 | Implemented |
| Wallet | [wallet-service/README.md](../wallet-service/README.md) | 8090 | Implemented |
| Blockchain Indexer | [blockchain-indexer-service/README.md](../blockchain-indexer-service/README.md) | 8091 | Implemented |

## Frontend

| App | Folder | Dev URL |
|-----|--------|---------|
| Investor Portal | [frontend/investor-portal/](../frontend/investor-portal/) | http://localhost:5173 |
| Admin Dashboard | [frontend/admin-dashboard/](../frontend/admin-dashboard/) | http://localhost:5174 |
| Tenant Portal | [frontend/tenant-portal/](../frontend/tenant-portal/) | http://localhost:5175 |

**12 backend services** · **3 frontend apps** · **6 shared libraries** — see [../README.md](../README.md).

See [rules/investor-portal.md](rules/investor-portal.md), [rules/admin-dashboard.md](rules/admin-dashboard.md), [rules/tenant-portal.md](rules/tenant-portal.md).

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
