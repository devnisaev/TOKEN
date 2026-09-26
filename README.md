# TokenRealty

Real-estate tokenization platform — fractional property ownership, crypto payments, rental dividends.

## Services

| Service | Folder | Port | Status |
|---------|--------|------|--------|
| Property Registry | `token-realty-app/` | 8081 | Implemented |
| Token Issuance | `token-issuance-service/` | 8082 | Implemented (Hardhat + Web3j) |
| Auth | `auth-service/` | 8083 | Implemented (JWT) |
| Marketplace | `marketplace-service/` | 8084 | Implemented (MVP) |
| Payment | `payment-service/` | 8085 | Implemented (MVP — escrow, payouts) |

## Documentation

| Doc | Description |
|-----|-------------|
| [docs/PLATFORM-SPEC.md](docs/PLATFORM-SPEC.md) | Platform spec & implementation TODO |
| [docs/EVENTS.md](docs/EVENTS.md) | Kafka event catalog |
| [docs/README.md](docs/README.md) | Docs index & diagrams |
| [docs/hardhat-demo.md](docs/hardhat-demo.md) | Local on-chain demo runbook |
| [AGENTS.md](AGENTS.md) | Agent / Cursor instructions |

## Cursor rules

| Rule | Description |
|------|-------------|
| [spring-java-services.mdc](.cursor/rules/spring-java-services.mdc) | Spring conventions, layering, API |
| [lombok.mdc](.cursor/rules/lombok.mdc) | Lombok on entities; records for DTOs |
| [java-dtos.mdc](.cursor/rules/java-dtos.mdc) | Typed request/response records |
| [kafka-messaging.mdc](.cursor/rules/kafka-messaging.mdc) | Kafka topics, outbox, consumers |
| [rest-client-errors.mdc](.cursor/rules/rest-client-errors.mdc) | Inter-service HTTP errors |
| [business-exception.mdc](.cursor/rules/business-exception.mdc) | ProblemDetail exceptions |
| [pagination.mdc](.cursor/rules/pagination.mdc) | List endpoint paging |
| [payment-ledger.mdc](.cursor/rules/payment-ledger.mdc) | Escrow, ledger, idempotency |
| [investment-limits.mdc](.cursor/rules/investment-limits.mdc) | KYC gates, min investment |

Human-readable expansions: [docs/rules/](docs/rules/)

## Local startup order

```bash
# 1. PostgreSQL
cd token-realty-app && docker compose up postgres -d

# 2. Create databases (once)
psql -U postgres -c "CREATE DATABASE property_registry;"
psql -U postgres -c "CREATE DATABASE token_issuance;"
psql -U postgres -c "CREATE DATABASE marketplace_service;"
psql -U postgres -c "CREATE DATABASE auth_service;"
psql -U postgres -c "CREATE DATABASE payment_service;"

# 3. Property Registry
cd token-realty-app && ./mvnw spring-boot:run

# 4. Hardhat (optional, for blockchain)
cd token-issuance-service/hardhat && npm run node

# 5. Token Issuance
cd token-issuance-service && ./mvnw spring-boot:run

# 6. Auth (JWT — required for all other services)
cd auth-service && ./mvnw spring-boot:run

# 7. Marketplace
cd marketplace-service && ./mvnw spring-boot:run

# 8. Payment
cd payment-service && ../token-realty-app/mvnw spring-boot:run
# Or from repo root:
# ./token-realty-app/mvnw -pl payment-service spring-boot:run
```

## Swagger UI

- Registry: http://localhost:8081/api/swagger-ui.html
- Issuance: http://localhost:8082/api/swagger-ui.html
- Auth: http://localhost:8083/api/swagger-ui.html
- Marketplace: http://localhost:8084/api/swagger-ui.html
- Payment: http://localhost:8085/api/swagger-ui.html

## Tests

From repo root (builds `tokenrealty-security` first):

```bash
./token-realty-app/mvnw test
```

Per service:

```bash
cd token-realty-app && ./mvnw test
cd token-issuance-service && ./mvnw test
cd auth-service && ./mvnw test
cd marketplace-service && ./mvnw test
cd payment-service && ../token-realty-app/mvnw test
```
