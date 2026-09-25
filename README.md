# TokenRealty

Real-estate tokenization platform — fractional property ownership, crypto payments, rental dividends.

## Services

| Service | Folder | Port | Status |
|---------|--------|------|--------|
| Property Registry | `token-realty-app/` | 8081 | Implemented |
| Token Issuance | `token-issuance-service/` | 8082 | Implemented (blockchain stubbed) |
| Marketplace | `marketplace-service/` | 8084 | Implemented (MVP) |
| Auth | `auth-service/` | 8083 | Implemented (JWT MVP) |
| Payment | `payment-service/` | 8085 | Planned |

## Documentation

| Doc | Description |
|-----|-------------|
| [docs/PLATFORM-SPEC.md](docs/PLATFORM-SPEC.md) | Platform spec & implementation TODO |
| [docs/EVENTS.md](docs/EVENTS.md) | Kafka event catalog |
| [docs/README.md](docs/README.md) | Docs index & diagrams |
| [AGENTS.md](AGENTS.md) | Agent / Cursor instructions |

## Cursor rules

| Rule | Description |
|------|-------------|
| [.cursor/rules/spring-java-services.mdc](.cursor/rules/spring-java-services.mdc) | Spring conventions, layering, API |
| [.cursor/rules/kafka-messaging.mdc](.cursor/rules/kafka-messaging.mdc) | Kafka topics, outbox, consumers |
| [.cursor/rules/rest-client-errors.mdc](.cursor/rules/rest-client-errors.mdc) | Inter-service HTTP errors |
| [.cursor/rules/business-exception.mdc](.cursor/rules/business-exception.mdc) | ProblemDetail exceptions |
| [.cursor/rules/pagination.mdc](.cursor/rules/pagination.mdc) | List endpoint paging |

## Local startup order

```bash
# 1. PostgreSQL
cd token-realty-app && docker compose up postgres -d

# 2. Create databases (once)
psql -U postgres -c "CREATE DATABASE property_registry;"
psql -U postgres -c "CREATE DATABASE token_issuance;"
psql -U postgres -c "CREATE DATABASE marketplace_service;"
psql -U postgres -c "CREATE DATABASE auth_service;"

# 3. Property Registry
cd token-realty-app && ./mvnw spring-boot:run

# 4. Hardhat (optional, for blockchain)
cd token-issuance-service/hardhat && npm run node

# 5. Token Issuance
cd token-issuance-service && ./mvnw spring-boot:run

# 6. Auth (JWT)
cd auth-service && ./mvnw spring-boot:run

# 7. Marketplace
cd marketplace-service && ./mvnw spring-boot:run

## Swagger UI

- Registry: http://localhost:8081/api/swagger-ui.html
- Issuance: http://localhost:8082/api/swagger-ui.html
- Auth: http://localhost:8083/api/swagger-ui.html
- Marketplace: http://localhost:8084/api/swagger-ui.html

## Tests

```bash
cd token-realty-app && ./mvnw test
cd token-issuance-service && ./mvnw test
cd auth-service && ./mvnw test
cd marketplace-service && ./mvnw test
```
