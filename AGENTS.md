# Agent Instructions — TokenRealty

Monorepo of Spring Boot 4 / Java 21 microservices for tokenized real estate.

## Stack

- Java 21, Maven, Spring Boot 4, PostgreSQL, JPA
- MapStruct, Lombok, springdoc OpenAPI
- Hardhat + Web3j (Token Issuance)
- Kafka + outbox (planned/enabled per service)

## Cursor rules

Read and follow rules in `.cursor/rules/`:

| Rule | When |
|------|------|
| [spring-java-services.mdc](.cursor/rules/spring-java-services.mdc) | Any Java / Spring work |
| [lombok.mdc](.cursor/rules/lombok.mdc) | Lombok on entities, services, tests |
| [java-dtos.mdc](.cursor/rules/java-dtos.mdc) | Request/response records, Kafka payloads |
| [kafka-messaging.mdc](.cursor/rules/kafka-messaging.mdc) | Kafka, events, outbox |
| [rest-client-errors.mdc](.cursor/rules/rest-client-errors.mdc) | RestClient / inter-service calls |
| [business-exception.mdc](.cursor/rules/business-exception.mdc) | Exceptions & ProblemDetail |
| [pagination.mdc](.cursor/rules/pagination.mdc) | List endpoints |
| [payment-ledger.mdc](.cursor/rules/payment-ledger.mdc) | Payment Service — escrow, ledger, idempotency |
| [investment-limits.mdc](.cursor/rules/investment-limits.mdc) | KYC gates, min investment, compliance order |
| [business-rules.mdc](.cursor/rules/business-rules.mdc) | Tier-1 invariants — read before money/Kafka features |
| [investor-portal.mdc](.cursor/rules/investor-portal.mdc) | React investor portal — Vite, gateway BFF, wagmi |
| [admin-dashboard.mdc](.cursor/rules/admin-dashboard.mdc) | React admin dashboard — buildings, KYC review |
| [tenant-portal.mdc](.cursor/rules/tenant-portal.mdc) | React tenant portal — lease view, rent payment |
| [commit-messages.mdc](.cursor/rules/commit-messages.mdc) | **Every commit** — `TOKEN-NNN:` prefix (change index) |

Human-readable expansions: [docs/rules/](docs/rules/) — incl. [commit-messages.md](docs/rules/commit-messages.md) — tier-1 invariants in [BUSINESS_RULES.md](docs/BUSINESS_RULES.md); cross-service modules in [shared-libraries.md](docs/rules/shared-libraries.md)

## Cursor skills (project)

| Skill | When |
|-------|------|
| [.cursor/skills/java-architect/SKILL.md](.cursor/skills/java-architect/SKILL.md) | Structure a service slice, options, plan before coding |
| [.cursor/skills/java-implementation/SKILL.md](.cursor/skills/java-implementation/SKILL.md) | Implement endpoints, Kafka, refactors — commands, outbox, exceptions |
| [.cursor/skills/java-code-review/SKILL.md](.cursor/skills/java-code-review/SKILL.md) | PR/diff review — layers, Kafka, exceptions, invariants |
| [.cursor/skills/java-debugging/SKILL.md](.cursor/skills/java-debugging/SKILL.md) | Red tests, Surefire/Mockito, Kafka parse, exception status mismatches |
| [.cursor/skills/java-testing/SKILL.md](.cursor/skills/java-testing/SKILL.md) | JUnit 5 / Mockito — service, controller, Kafka consumer tests |
| [.cursor/skills/solidity-smart-contract/SKILL.md](.cursor/skills/solidity-smart-contract/SKILL.md) | Solidity contracts, Hardhat deploy, security review |
| [.cursor/skills/kafka-event-architect/SKILL.md](.cursor/skills/kafka-event-architect/SKILL.md) | Kafka topics, outbox, listeners, payload records |
| [.cursor/skills/rwa-legal-compliance/SKILL.md](.cursor/skills/rwa-legal-compliance/SKILL.md) | KYC gates, SPV metadata, IPFS data room, jurisdiction |
| [.cursor/skills/web3j-blockchain-integration/SKILL.md](.cursor/skills/web3j-blockchain-integration/SKILL.md) | Web3j, RPC, receipts, Payment/Issuance blockchain |

## Services

| Service | Port | Package |
|---------|------|---------|
| API Gateway | 8080 | `com.tokenrealty.gateway` |
| Property Registry | 8081 | `com.tokenrealty.registry` |
| Token Issuance | 8082 | `com.tokenrealty.issuance` |
| Auth | 8083 | `com.tokenrealty.auth` |
| Marketplace | 8084 | `com.tokenrealty.marketplace` |
| Payment | 8085 | `com.tokenrealty.payment` |
| Notification | 8089 | `com.tokenrealty.notification` |
| Rental | 8086 | `com.tokenrealty.rental` |
| Compliance | 8087 | `com.tokenrealty.compliance` |
| Document | 8088 | `com.tokenrealty.document` |
| Wallet | 8090 | `com.tokenrealty.wallet` |
| Blockchain Indexer | 8091 | `com.tokenrealty.indexer` |
| Reporting / Analytics | 8093 | `com.tokenrealty.reporting` |
| Settlement / Saga Tracker | 8094 | `com.tokenrealty.settlement` |

### Phase 6 — remaining (planned)

See [docs/PLATFORM-SPEC.md §12](docs/PLATFORM-SPEC.md#12-phase-6--planned-services).

| Service | Port | Package (proposed) | Status |
|---------|------|-------------------|--------|
| Valuation / NAV | 8095 | `com.tokenrealty.valuation` | Planned |
| Audit Ledger | 8096 | `com.tokenrealty.audit` | Planned |
| Corporate Actions | 8097 | `com.tokenrealty.corporateactions` | Planned |
| Search | 8098 | `com.tokenrealty.search` | Planned |
| Integration Hub | 8099 | `com.tokenrealty.integration` | Planned |

**Do not spin out:** limits/policy service, config service, Payment escrow, Issuance deploy/transfer core.

New services: copy structure from `marketplace-service/` or `auth-service/`.

## Frontend

| App | Folder | Dev URL |
|-----|--------|---------|
| Investor Portal | `frontend/investor-portal/` | http://localhost:5173 |
| Admin Dashboard | `frontend/admin-dashboard/` | http://localhost:5174 |
| Tenant Portal | `frontend/tenant-portal/` | http://localhost:5175 |

Stack: React 19, TypeScript, Vite, TanStack Query, wagmi (investor only), Tailwind. All API via gateway `:8080`.

Docs: [docs/rules/investor-portal.md](docs/rules/investor-portal.md), [docs/rules/admin-dashboard.md](docs/rules/admin-dashboard.md), [docs/rules/tenant-portal.md](docs/rules/tenant-portal.md), [docs/rules/api-gateway-bff.md](docs/rules/api-gateway-bff.md).  
Cursor rules: [investor-portal.mdc](.cursor/rules/investor-portal.mdc), [admin-dashboard.mdc](.cursor/rules/admin-dashboard.mdc), [tenant-portal.mdc](.cursor/rules/tenant-portal.mdc).

```bash
cd frontend/investor-portal && npm install && npm run dev
cd frontend/admin-dashboard && npm install && npm run dev
cd frontend/tenant-portal && npm install && npm run dev
```

## Run tests

```bash
./token-realty-app/mvnw test   # from repo root (builds tokenrealty-security first)
./mvnw test                    # inside each service folder (after security lib installed)
```

Shared libs: `tokenrealty-security/` (JWT, `ServiceRestClientBuilder`), `tokenrealty-web/` (exceptions, RFC 7807 handler), `tokenrealty-jpa/` (`BaseEntity`, auditing), `tokenrealty-kafka/` (`KafkaEventConsumer`, processed-event idempotency), `tokenrealty-events/` (envelope, `KafkaJsonEvent`), `tokenrealty-outbox/` (`OutboxWriter`, `OutboxRelay`, `OutboxPayload`) — `./token-realty-app/mvnw -pl tokenrealty-security,tokenrealty-web,tokenrealty-jpa,tokenrealty-kafka,tokenrealty-events,tokenrealty-outbox install

OpenAPI codegen: `cd frontend/openapi && npm install && npm run codegen` → `frontend/shared-api-types/``

## Platform docs

- Spec & TODO: [docs/PLATFORM-SPEC.md](docs/PLATFORM-SPEC.md)
- Kafka events: [docs/EVENTS.md](docs/EVENTS.md)
- Diagrams: [docs/diagrams/](docs/diagrams/)

## Conventions

- Controllers: validate → service → map; no business logic
- `@Transactional` on service write methods; no blockchain/HTTP inside long TX
- RFC 7807 ProblemDetail for errors
- Records for DTOs; secrets from env only
- Implement only what is requested; minimal diff
