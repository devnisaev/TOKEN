# Property Registry Service

Microservice for the **TokenRealty** platform — manages buildings, flats, SPVs, valuations, and legal documents as the source of truth before tokenization.

---

## Quick Start

### 1. Start PostgreSQL
```bash
docker compose up postgres -d
```

### 2. Run the app
```bash
./mvnw spring-boot:run
```

### 3. Open Swagger UI
```
http://localhost:8081/api/swagger-ui.html
```

---

## Authentication

JWT Bearer tokens from **Auth Service** (:8083). Login at `POST /api/v1/auth/login`, then send `Authorization: Bearer <token>` on all `/v1/**` requests.

| Email | Password | Role |
|-------|----------|------|
| admin@tokenrealty.com | admin123 | ADMIN |
| manager@tokenrealty.com | manager123 | PROPERTY_MANAGER |
| investor@tokenrealty.com | investor123 | INVESTOR |

---

## Running Tests

### Unit tests only (fast, no DB needed)
```bash
./mvnw test -Dgroups="unit"
```

### All tests with H2 in-memory DB
```bash
./mvnw test -Dspring.profiles.active=test
```

### Full test suite
```bash
./mvnw verify
```

---

## API Overview

| Method   | Endpoint                                      | Role Required        |
|----------|-----------------------------------------------|----------------------|
| GET      | `/v1/buildings`                               | Any authenticated    |
| GET      | `/v1/buildings/{id}`                          | Any authenticated    |
| POST     | `/v1/buildings`                               | ADMIN, PROPERTY_MGR  |
| PUT      | `/v1/buildings/{id}`                          | ADMIN, PROPERTY_MGR  |
| PATCH    | `/v1/buildings/{id}/status`                   | ADMIN                |
| DELETE   | `/v1/buildings/{id}`                          | ADMIN                |
| GET      | `/v1/buildings/{id}/flats`                    | Any authenticated    |
| POST     | `/v1/buildings/{id}/flats`                    | ADMIN, PROPERTY_MGR  |
| PATCH    | `/v1/flats/{id}/token-info`                   | ADMIN                |
| POST     | `/v1/buildings/{id}/spv`                      | ADMIN                |
| PATCH    | `/v1/buildings/{id}/spv/{spvId}/kyc`          | ADMIN, COMPLIANCE    |
| POST     | `/v1/flats/{id}/valuations`                   | ADMIN, APPRAISER     |
| GET      | `/v1/flats/{id}/valuations/current`           | Any authenticated    |
| POST     | `/v1/buildings/{id}/documents`                | ADMIN, PROPERTY_MGR  |
| PATCH    | `/v1/documents/{id}/verify`                   | ADMIN, COMPLIANCE    |

---

## Architecture

```
Controller → Service → Repository → PostgreSQL
                ↓
          MapStruct DTOs
                ↓
          Liquibase migrations (001–005)
```

## Project Structure

```
src/main/java/com/tokenrealty/registry/
├── controller/      REST controllers (Building, Flat, SPV, Valuation, Document)
├── service/         Business logic
├── repository/      JPA repositories
├── entity/          JPA entities (Building, Flat, SpvEntity, Valuation, PropertyDocument)
├── dto/             Request/Response records
├── mapper/          MapStruct mappers
├── exception/       Custom exceptions + GlobalExceptionHandler
└── config/          SecurityConfig

src/main/resources/
├── application.yml
└── db/changelog/    Liquibase migrations (001–005)

src/test/
├── service/         Unit tests with Mockito (all 5 services)
├── controller/      MockMvc WebMvcTest for BuildingController
└── integration/     Full H2 integration test (17 ordered scenarios)
```

---

## Next Steps

- [ ] Connect to **Token Issuance Service** — call `PATCH /flats/{id}/token-info` after contract deployment
- [ ] Add **Kafka events** — publish `FlatTokenized`, `BuildingApproved` events
- [ ] Replace in-memory users with **JWT filter** from Auth Service
- [ ] Connect **IPFS** client for actual document upload
- [ ] Add **Testcontainers** integration test against real PostgreSQL