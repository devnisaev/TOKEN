# API Gateway

Reverse proxy and BFF layer for **TokenRealty** — single browser entry point on port **8080**.

## Responsibilities

- Route `/api/**` to downstream microservices
- JWT validation at the edge
- CORS for frontend dev ports (`5173`, `5174`, `5175`)
- BFF aggregates: `GET /api/v1/bff/flats/{id}`, `GET /api/v1/bff/listings/{id}`
- Platform health: `GET /actuator/platform-health`

## Run

```bash
./mvnw spring-boot:run
```

Requires Auth Service (:8083) for JWT validation. See [docs/rules/api-gateway-bff.md](../docs/rules/api-gateway-bff.md).
