# API Gateway

Reverse proxy and BFF layer for **TokenRealty** — single browser entry point on port **8080**.

## Responsibilities

- Route `/api/**` to downstream microservices
- JWT validation at the edge
- CORS for frontend dev ports (`5173`, `5174`, `5175`)
- BFF aggregates for portal pages
- Platform health: `GET /actuator/platform-health`
- Per-IP rate limiting (in-memory MVP)
- BFF response caching (Caffeine, 60s TTL for flat/listing detail)

## BFF API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/bff/flats/{flatId}` | Registry flat + Issuance contract + active listing |
| GET | `/api/v1/bff/listings/{listingId}` | Listing + flat summary + contract |
| GET | `/api/v1/bff/buildings/{buildingId}` | Building + SPV + tokenized/available counts |
| GET | `/api/v1/bff/investors/{investorId}/portfolio` | Wallet balance + holdings + recent dividends |
| GET | `/api/v1/bff/orders/{orderId}/status-stream` | SSE order + trade status (investor portal) |

All BFF routes require investor JWT unless noted in [docs/rules/api-gateway-bff.md](../docs/rules/api-gateway-bff.md).

## Proxy routes

Downstream services are proxied under `/api/v1/...` (Auth, Registry, Marketplace, Payment, etc.). See gateway `application.yml` route table.

## Run

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Requires Auth Service (:8083) for JWT validation.

Docker: `./scripts/docker-build.sh api-gateway 8080`

## BFF caching

`GET /bff/flats/{id}` and `GET /bff/listings/{id}` are cached in-memory (`bff-flats`, `bff-listings`). TTL defaults to **60s** — override with `GATEWAY_BFF_CACHE_TTL`. Requires `spring-boot-starter-cache` + Caffeine on the classpath.

See [docs/rules/api-gateway-bff.md](../docs/rules/api-gateway-bff.md).
