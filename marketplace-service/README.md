# Marketplace Service

Microservice for **TokenRealty** — primary and secondary market for tokenized property fractions.

Port **8084** · Database **`marketplace_service`**

## Responsibilities

- List tokenized flats for sale (primary market)
- Accept investor buy orders with KYC compliance check
- Reserve tokens and create matched orders + pending trades
- Emit outbox events for Payment and Token Issuance (Kafka when enabled)
- Settle trades after payment and on-chain transfer (admin callback until Marketplace ↔ Payment wired)

## Architecture

```
REST API (:8084)
    ↓
ListingService / OrderService
    ↓
PostgreSQL (listings, orders, trades, outbox_events)
    ↓
Token Issuance (:8082) — GET /v1/compliance/check/{wallet}
    ↓
Kafka (optional) — listing.created, order.matched, trade.settled
```

## Quick start

### Prerequisites

- Java 21
- PostgreSQL
- Property Registry + Token Issuance running (for full flow)

### 1. Create database

```bash
psql -U postgres -c "CREATE DATABASE marketplace_service;"
```

### 2. Run the service

```bash
./mvnw spring-boot:run
```

### 3. Swagger UI

```
http://localhost:8084/api/swagger-ui.html
```

## Authentication

JWT Bearer from Auth Service (:8083). Example: login as `investor@tokenrealty.com` / `investor123`, then `Authorization: Bearer <token>`.

## API Overview

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| GET | `/v1/listings` | Any | Search listings |
| GET | `/v1/listings/{id}` | Any | Get listing |
| POST | `/v1/listings` | ADMIN, PROPERTY_MANAGER | Create listing |
| PATCH | `/v1/listings/{id}/cancel` | ADMIN, PROPERTY_MANAGER | Cancel listing |
| GET | `/v1/orders` | Any | List orders (`?buyerId=`, `?listingId=`, `?status=`) |
| GET | `/v1/orders/{id}` | Any | Get order |
| POST | `/v1/orders` | INVESTOR, ADMIN | Place buy order |
| POST | `/v1/orders/sell` | INVESTOR, ADMIN | Place secondary sell order (creates listing) |
| GET | `/v1/orders/{id}/trade` | Any | Get trade for order |
| PATCH | `/v1/orders/{id}/settle` | ADMIN | Mark settled (fallback when Kafka auto-settle disabled) |
| POST | `/v1/listings/secondary` | INVESTOR, ADMIN | Create secondary listing for held tokens |

## Buy flow (MVP)

```
1. Admin creates listing for tokenized flat
2. Investor POST /v1/orders with buyerWallet + tokenAmount
3. Marketplace checks KYC via Token Issuance
4. Tokens reserved; order MATCHED; trade PENDING
5. Marketplace calls Payment Service — escrow created, `trade.paymentId` set
6. Investor sends USDC to escrow wallet; admin/service confirms on-chain tx
7. payment.confirmed → Issuance transfers tokens → transfer.completed → auto-settle + escrow release
```

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `services.token-issuance.url` | `http://localhost:8082/api` | Compliance check |
| `services.payment.url` | `http://localhost:8085/api` | Escrow on order match |
| `tokenrealty.service-account.client-id` | `marketplace` | Service token for Payment API |
| `tokenrealty.kafka.enabled` | `false` | Enqueue outbox events |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka brokers |

## Tests

```bash
./mvnw test
```

## Next steps

- [x] Kafka relay for outbox → broker (`OutboxRelayWorker` + `OutboxPayload`)
- [x] Consumer: `flat.tokenized` auto-create listing
- [x] Payment Service integration — `PaymentClient` initiates escrow on order match
- [x] Consume `payment.confirmed` + `transfer.completed` to auto-settle trades
- [x] Secondary market sell orders (`POST /v1/orders/sell`, `POST /v1/listings/secondary`)

## Conventions

Follow [`.cursor/rules/spring-java-services.mdc`](../.cursor/rules/spring-java-services.mdc) — this service is the template for new TokenRealty microservices.

See also:

- [docs/PLATFORM-SPEC.md](../docs/PLATFORM-SPEC.md) §10.4
- [docs/EVENTS.md](../docs/EVENTS.md)
- [docs/rules/spring-java-services.md](../docs/rules/spring-java-services.md)
