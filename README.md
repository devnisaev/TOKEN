# TokenRealty

Real-estate tokenization platform — fractional property ownership, crypto payments, rental dividends.

## Platform inventory

| Layer | Count | Notes |
|-------|-------|-------|
| Backend microservices | **12** | Spring Boot 4 / Java 21 |
| Frontend apps | **3** | React 19 + Vite (investor, admin, tenant) |
| Shared Maven libraries | **6** | security, web, jpa, kafka, events, outbox |

## Backend services

| Service | Folder | Port | Status |
|---------|--------|------|--------|
| API Gateway | `api-gateway/` | 8080 | Implemented (proxy + BFF) |
| Property Registry | `token-realty-app/` | 8081 | Implemented |
| Token Issuance | `token-issuance-service/` | 8082 | Implemented (Hardhat + Web3j) |
| Auth | `auth-service/` | 8083 | Implemented (JWT) |
| Marketplace | `marketplace-service/` | 8084 | Implemented |
| Payment | `payment-service/` | 8085 | Implemented (escrow, ledger, payouts) |
| Rental | `rental-service/` | 8086 | Implemented (leases, rent payments) |
| Compliance | `compliance-service/` | 8087 | Implemented (KYC whitelist) |
| Document | `document-service/` | 8088 | Implemented (IPFS upload) |
| Notification | `notification-service/` | 8089 | Implemented (email + Kafka) |
| Wallet | `wallet-service/` | 8090 | Implemented (custodial + aggregate balance) |
| Blockchain Indexer | `blockchain-indexer-service/` | 8091 | Implemented (on-chain sync) |

## Shared libraries

| Library | Purpose |
|---------|---------|
| `tokenrealty-security/` | JWT filter, service account RestClient |
| `tokenrealty-web/` | RFC 7807 exceptions, ProblemDetail handler |
| `tokenrealty-jpa/` | `BaseEntity`, auditing |
| `tokenrealty-kafka/` | `KafkaEventConsumer`, processed-event dedupe |
| `tokenrealty-events/` | Event envelope, `KafkaJsonEvent` |
| `tokenrealty-outbox/` | `OutboxWriter`, `OutboxRelay` |

Install shared libs (once):

```bash
./token-realty-app/mvnw -pl tokenrealty-security,tokenrealty-web,tokenrealty-jpa,tokenrealty-kafka,tokenrealty-events,tokenrealty-outbox install
```

## Frontend apps

| App | Folder | Dev URL |
|-----|--------|---------|
| Investor Portal | `frontend/investor-portal/` | http://localhost:5173 |
| Admin Dashboard | `frontend/admin-dashboard/` | http://localhost:5174 |
| Tenant Portal | `frontend/tenant-portal/` | http://localhost:5175 |

All API traffic goes through **API Gateway** `:8080`.

```bash
cd frontend/investor-portal && npm install && npm run dev
cd frontend/admin-dashboard && npm install && npm run dev
cd frontend/tenant-portal && npm install && npm run dev
```

## Documentation

| Doc | Description |
|-----|-------------|
| [docs/PLATFORM-SPEC.md](docs/PLATFORM-SPEC.md) | Platform spec & implementation TODO |
| [docs/EVENTS.md](docs/EVENTS.md) | Kafka event catalog |
| [docs/README.md](docs/README.md) | Docs index & service links |
| [docs/hardhat-demo.md](docs/hardhat-demo.md) | Local on-chain demo runbook |
| [AGENTS.md](AGENTS.md) | Agent / Cursor instructions |

## Local startup order

### Quick demo (recommended)

```bash
./scripts/demo-start.sh              # Postgres + Kafka + Jaeger + checklist
./scripts/demo-services.sh           # background buy-flow stack (auth → gateway)
./scripts/wait-for-services.sh     # wait until :8080 health is OK
./scripts/seed-demo.sh               # demo buildings, flats, users
./scripts/e2e-run.sh --no-wait       # Playwright full E2E (gateway must be up)
```

Stop background services: `./scripts/demo-services.sh --stop`

### Manual startup (separate terminals)

```bash
# 1. PostgreSQL (+ optional Kafka)
./scripts/demo-start.sh --infra-only

# 2. Auth first (JWT for all services)
cd auth-service && ../token-realty-app/mvnw spring-boot:run -Dspring-boot.run.profiles=local

# 3. Core domain services
cd token-realty-app && ../token-realty-app/mvnw spring-boot:run -Dspring-boot.run.profiles=local          # Registry :8081
cd token-issuance-service && ../token-realty-app/mvnw spring-boot:run -Dspring-boot.run.profiles=local    # Issuance :8082
cd marketplace-service && ../token-realty-app/mvnw spring-boot:run -Dspring-boot.run.profiles=local       # Marketplace :8084
cd payment-service && ../token-realty-app/mvnw spring-boot:run -Dspring-boot.run.profiles=local           # Payment :8085
cd compliance-service && ../token-realty-app/mvnw spring-boot:run -Dspring-boot.run.profiles=local        # Compliance :8087

# 4. API Gateway (browser entry point)
cd api-gateway && ../token-realty-app/mvnw spring-boot:run -Dspring-boot.run.profiles=local               # :8080

# 5. Optional: Hardhat node for on-chain flows
cd token-issuance-service/hardhat && npm run node && npm run deploy:local
```

See [docs/hardhat-demo.md](docs/hardhat-demo.md) for tokenize → list → buy flow.

## Swagger UI (direct service ports)

| Service | URL |
|---------|-----|
| Gateway BFF | http://localhost:8080/api/v1/bff/… (via gateway) |
| Registry | http://localhost:8081/api/swagger-ui.html |
| Issuance | http://localhost:8082/api/swagger-ui.html |
| Auth | http://localhost:8083/api/swagger-ui.html |
| Marketplace | http://localhost:8084/api/swagger-ui.html |
| Payment | http://localhost:8085/api/swagger-ui.html |
| Rental | http://localhost:8086/api/swagger-ui.html |
| Compliance | http://localhost:8087/api/swagger-ui.html |
| Document | http://localhost:8088/api/swagger-ui.html |
| Notification | http://localhost:8089/api/swagger-ui.html |
| Wallet | http://localhost:8090/api/swagger-ui.html |
| Indexer | http://localhost:8091/api/swagger-ui.html |

## Demo credentials

| Email | Password | Role |
|-------|----------|------|
| `investor@tokenrealty.com` | `investor123` | INVESTOR |
| `admin@tokenrealty.com` | `admin123` | ADMIN |
| `tenant@tokenrealty.com` | `tenant123` | TENANT |
| `compliance@tokenrealty.com` | `compliance123` | COMPLIANCE |

## Tests

From repo root (builds shared libs first):

```bash
./token-realty-app/mvnw test
```

Per service:

```bash
cd <service-folder> && ./mvnw test
```
