# API Gateway BFF — TokenRealty

**Status:** BFF aggregate endpoints implemented in `api-gateway/` alongside the reverse proxy.

Backend-for-Frontend layer reduces N+1 client calls for web/mobile apps. Investor Portal is the first consumer.

Related: [investor-portal.md](investor-portal.md) · [spring-java-services.md](spring-java-services.md)

---

## Architecture

```text
Browser (:5173)
    ↓  /api/v1/bff/**
API Gateway (:8080)
    ├─ BffController        ← handled locally (not proxied)
    └─ GatewayProxyController ← /api/** → downstream services
```

Service account: `api-gateway` / `gateway-secret` (ADMIN) — used by BFF RestClients to call Registry, Marketplace, Issuance.

---

## BFF endpoints

| Method | Path | Composes |
|--------|------|----------|
| `GET` | `/v1/bff/flats/{flatId}` | Registry flat + Issuance contract by flat + active Marketplace listing |
| `GET` | `/v1/bff/listings/{listingId}` | Marketplace listing + Registry flat summary + Issuance contract |

Same JWT as other gateway routes — investor token required.

---

## Downstream calls (server-side)

| BFF method | Registry | Marketplace | Issuance |
|------------|----------|-------------|----------|
| `getFlatDetail` | `GET /v1/flats/{id}` | `GET /v1/listings?flatId=&status=ACTIVE` | `GET /v1/tokens/by-flat/{flatId}` |
| `getListingDetail` | `GET /v1/flats/{id}` | `GET /v1/listings/{id}` | `GET /v1/tokens/by-flat/{flatId}` |

404 on Issuance contract returns `null` in aggregate (flat may not be tokenized yet).

---

## Package layout

```
api-gateway/src/main/java/com/tokenrealty/gateway/
├── bff/
│   ├── BffController.java
│   ├── BffFlatService.java
│   └── BffListingService.java
├── client/
│   ├── PropertyRegistryClient.java
│   ├── MarketplaceClient.java
│   └── TokenIssuanceClient.java
├── config/ServiceClientConfig.java
└── dto/BffDtos.java
```

---

## Proxy routes (frontend-relevant)

| Path prefix | Target | Used by portal |
|-------------|--------|---------------|
| `/api/v1/auth`, `/api/v1/users` | Auth `:8083` | Login, profile, wallet |
| `/api/v1/listings`, `/api/v1/orders` | Marketplace `:8084` | Listings, buy orders |
| `/api/v1/wallets` | Wallet `:8090` | Portfolio aggregate |
| `/api/v1/wallet-balances` | Payment `:8085` | (via Wallet service) |
| `/api/v1/bff` | Gateway local | Listing/flat detail |

---

## Configuration

```yaml
# api-gateway application.yml
services:
  property-registry:
    url: ${PROPERTY_REGISTRY_URL:http://localhost:8081/api}
  token-issuance:
    url: ${TOKEN_ISSUANCE_URL:http://localhost:8082/api}
  marketplace:
    url: ${MARKETPLACE_SERVICE_URL:http://localhost:8084/api}

tokenrealty:
  service-account:
    client-id: api-gateway
    client-secret: ${SERVICE_ACCOUNT_SECRET:gateway-secret}
```

---

## Adding a new BFF endpoint

1. Add client method(s) in `gateway/client/` with error mapping (`ValidationException` on 5xx)
2. Add compose logic in `bff/Bff*Service.java`
3. Expose on `BffController` under `/api/v1/bff/...`
4. Ensure path does **not** match a proxy-only prefix before local controller
5. Document in this file + update `investor-portal` types if consumed by UI

---

## Pending / future

- [ ] `GET /v1/bff/investors/{id}/portfolio` — thin wrapper over Wallet balance
- [ ] `GET /v1/bff/buildings/{id}` — building + SPV + tokenized flat count
- [ ] Response caching (short TTL) for public listing pages
- [ ] GraphQL layer (optional; REST BFF sufficient for MVP)
