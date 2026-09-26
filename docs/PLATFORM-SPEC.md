# TokenRealty Platform — Implementation Spec & TODO

> **Version:** 1.3  
> **Date:** 2026-09-26  
> **Status:** Phases 0–6 complete (tracks 1–402); Phase 7 in progress (tracks 403+)  
> **Purpose:** Master specification and implementation backlog for the TokenRealty real-estate tokenization platform (buy, sell, rent with cryptocurrency).

---

## Table of Contents

1. [Vision](#1-vision)
2. [Current State](#2-current-state)
3. [Target Architecture](#3-target-architecture)
4. [Microservices Catalog](#4-microservices-catalog)
5. [Domain Flows — Sell & Rent](#5-domain-flows--sell--rent)
6. [Event-Driven Integration (Kafka)](#6-event-driven-integration-kafka)
7. [Service Port & Naming Convention](#7-service-port--naming-convention)
8. [Implementation Phases](#8-implementation-phases)
9. [Foundation Fixes (Phase 0)](#9-foundation-fixes-phase-0)
10. [Per-Service TODO Checklists](#10-per-service-todo-checklists)
11. [Cross-Cutting Concerns](#11-cross-cutting-concerns)
12. [Phase 6 — Planned Services](#12-phase-6--planned-services)
13. [Phase 7 — Event Mesh & Production Integrations](#13-phase-7--event-mesh--production-integrations)
14. [Diagram Index](#14-diagram-index)
15. [Open Questions & Decisions](#15-open-questions--decisions)
16. [Cursor Rules & Coding Standards](#16-cursor-rules--coding-standards)

---

## 1. Vision

TokenRealty tokenizes real estate assets (buildings, flats, and other property types) so investors can **buy fractional ownership** and **earn rental income** using cryptocurrency (USDC, MATIC, ETH).

### Core capabilities (target)

| Capability | Description |
|------------|-------------|
| **Asset registry** | Buildings, flats, SPVs, valuations, legal documents as source of truth |
| **Tokenization** | ERC-1400 property tokens deployed on Polygon per flat |
| **Primary market** | Investors buy tokens with crypto |
| **Secondary market** | Peer-to-peer token trading with compliance gates |
| **Rental operations** | Lease management, rent collection in crypto, pro-rata dividends |
| **Compliance** | KYC/AML whitelist (off-chain + on-chain) |
| **Documents** | Title deeds, leases stored on IPFS with verifiable CIDs |

---

## 2. Current State

### Existing services (14 backend microservices)

| Service | Folder | Port | Database | Status |
|---------|--------|------|----------|--------|
| API Gateway | `api-gateway/` | 8080 | — | Implemented (proxy + BFF) |
| Property Registry | `token-realty-app/` | 8081 | `property_registry` | Implemented |
| Token Issuance | `token-issuance-service/` | 8082 | `token_issuance` | Implemented (Hardhat + Web3j) |
| Auth | `auth-service/` | 8083 | `auth_service` | Implemented (JWT) |
| Marketplace | `marketplace-service/` | 8084 | `marketplace_service` | Implemented |
| Payment | `payment-service/` | 8085 | `payment_service` | Implemented |
| Rental | `rental-service/` | 8086 | `rental_service` | Implemented |
| Compliance | `compliance-service/` | 8087 | `compliance_service` | Implemented |
| Document | `document-service/` | 8088 | `document_service` | Implemented |
| Notification | `notification-service/` | 8089 | `notification_service` | Implemented |
| Wallet | `wallet-service/` | 8090 | `wallet_service` | Implemented |
| Blockchain Indexer | `blockchain-indexer-service/` | 8091 | `blockchain_indexer` | Implemented |
| Reporting / Analytics | `reporting-service/` | 8093 | `reporting_service` | Implemented |
| Settlement / Saga Tracker | `settlement-service/` | 8094 | `settlement_service` | Implemented |
| Valuation / NAV | `valuation-service/` | 8095 | `valuation_service` | Implemented |
| Audit Ledger | `audit-ledger-service/` | 8096 | `audit_ledger_service` | Implemented |
| Corporate Actions | `corporate-actions-service/` | 8097 | `corporate_actions_service` | Implemented |
| Search | `search-service/` | 8098 | `search_service` | Implemented |
| Integration Hub | `integration-hub-service/` | 8099 | `integration_hub_service` | Implemented |

### What works today

- [x] Property catalog CRUD (buildings, flats, SPV, valuations, documents metadata)
- [x] Token issuance orchestration (API layer)
- [x] Compliance CRUD + verify/revoke (DB layer)
- [x] Admin-initiated token transfers with compliance checks
- [x] Holder registry and dividend calculation (pro-rata)
- [x] Inter-service call: Issuance → Registry (`PropertyRegistryClient`)
- [x] Unit and integration tests in both services
- [x] **Marketplace** — listings, buy orders, KYC check via Issuance, outbox events, unit tests

### What is stubbed or missing

- [x] **Smart contracts** — `PropertyToken.sol`, `ComplianceRegistry.sol`, `MockUSDC.sol` in `hardhat/contracts/` (compliance-gated ERC-20 MVP)
- [x] **Per-flat deploy script** — `hardhat/scripts/deployFlat.js` wired to `ContractDeployer`
- [x] **On-chain dividends** — Issuance `dividend.distributed` → Payment USDC → `payout.completed` → Issuance `DividendPayment` PAID
- [x] **Inter-service auth** — JWT via `tokenrealty-security`; service tokens on RestClient
- [x] **Liquibase** — Registry changelogs exist; enable via `--spring.profiles.active=prod`
- [x] **Issuance Liquibase** — `db/changelog/001-initial-schema.xml`; enable via `prod` profile
- [x] **Kafka** — outbox relay + consumers in Marketplace, Payment, Issuance, Compliance, Rental
- [x] **Auth service** — JWT MVP; registry/issuance/marketplace validate Bearer tokens
- [x] **IPFS upload** — Document Service multipart → IPFS → Registry CID
- [x] **Payment** — MVP (escrow, confirm, release, payouts, outbox stub)
- [x] **Rental** — MVP (leases, rent-payments, occupancy; Kafka rent → dividend pipeline)
- [x] **Wallet** — custodial wallets, aggregate balance, Payment sync
- [x] **Blockchain Indexer** — on-chain event poll + reconciliation
- [x] **API Gateway** — reverse proxy + BFF aggregates
- [x] **Root README / platform docs** — [README.md](../README.md), [AGENTS.md](../AGENTS.md), Cursor rules
- [x] **`FULLY_SOLD` status** — Marketplace → Registry on primary sell-out

### Key file references

| Concern | Path |
|---------|------|
| Registry entities | `token-realty-app/src/main/java/com/tokenrealty/registry/entity/` |
| Issuance entities | `token-issuance-service/src/main/java/com/tokenrealty/issuance/entity/` |
| Inter-service client | `token-issuance-service/src/main/java/com/tokenrealty/issuance/client/PropertyRegistryClient.java` |
| Blockchain layer | `token-issuance-service/src/main/java/com/tokenrealty/issuance/blockchain/` |
| Smart contracts | `token-issuance-service/hardhat/contracts/` |
| Hardhat | `token-issuance-service/hardhat/` |
| Registry Liquibase | `token-realty-app/src/main/resources/db/changelog/` |
| Marketplace entities | `marketplace-service/src/main/java/com/tokenrealty/marketplace/entity/` |
| Marketplace README | `marketplace-service/README.md` |
| Cursor rules | `.cursor/rules/*.mdc` |
| Coding standards (human) | `docs/rules/spring-java-services.md` |

---

## 3. Target Architecture

See diagram: [`diagrams/02-target-architecture.puml`](diagrams/02-target-architecture.puml)

```
Clients (Investor Portal, Admin Dashboard)
    ↓
API Gateway / BFF (:8080)
    ↓
┌─────────────────────────────────────────────────────────────┐
│  Auth (:8083)  │  Property Registry (:8081)  │  Token Issuance (:8082)  │
│  Marketplace (:8084)  │  Payment (:8085)  │  Rental (:8086)           │
│  Compliance/KYC (:8087)  │  Document/IPFS (:8088)  │  Notification     │
└─────────────────────────────────────────────────────────────┘
    ↓                              ↓
Kafka Event Bus              PostgreSQL (per service)
    ↓                              ↓
Notification Service         Polygon / Hardhat + IPFS
```

---

## 4. Microservices Catalog

### Tier 1 — Unblock everything (build first)

#### 4.1 Auth Service (`auth-service`, :8083) — **MVP implemented**

**Why:** Replace in-memory Basic auth; enable secure inter-service communication.

**Status:** JWT register/login/refresh, service tokens, user profile. See [auth-service/README.md](../auth-service/README.md). Other services not yet on JWT validation.

| Responsibility | Details |
|----------------|---------|
| User registration & login | Email/password, optional OAuth2 |
| JWT issuance & validation | Access + refresh tokens |
| Role management | `ADMIN`, `PROPERTY_MANAGER`, `APPRAISER`, `COMPLIANCE`, `INVESTOR`, `TENANT` |
| Service accounts | Internal tokens for Issuance → Registry, etc. |
| Wallet linking | Map `investorId` ↔ blockchain wallet address |

**Integrates with:** All services (JWT resource server or shared auth library).

---

#### 4.2 Payment Service (`payment-service`, :8085) — **MVP implemented**

**Why:** Core to "sell/rent with crypto."

**Status:** Escrow, confirm, release, payouts, ledger entries, outbox stub. See [payment-service/README.md](../payment-service/README.md). Marketplace auto-initiates escrow on order match via `PaymentClient`.

| Responsibility | Details |
|----------------|---------|
| Crypto payments | Accept USDC, MATIC, ETH for token purchases |
| Escrow | Hold funds until on-chain transfer confirmed |
| Rent collection | Tenant rent in stablecoin → SPV wallet |
| Dividend payouts | Replace simulated dividends with real crypto transfers |
| Ledger | Transaction history, reconciliation with on-chain state |
| Fiat on-ramp (later) | Stripe, MoonPay integration hook |

**Key entities:** `Payment`, `Escrow`, `Payout`, `WalletBalance`

**DB:** `payment_service` PostgreSQL database

---

#### 4.3 Marketplace Service (`marketplace-service`, :8084) — **MVP implemented**

**Why:** Investors need discovery, listings, and order flow for primary and secondary markets.

**Status:** Listings, buy orders, KYC gate, outbox events, interim admin settle. See [marketplace-service/README.md](../marketplace-service/README.md).

| Responsibility | Details |
|----------------|---------|
| Primary market | List tokenized flats with price, availability, min investment |
| Secondary market | Sell orders, buy orders, order matching |
| Compliance gate | Call Compliance Service `/v1/compliance/check/{wallet}` before trade |
| Checkout | Reserve tokens during payment (integrate with Payment Service) |

**Key entities:** `Listing`, `Order`, `Trade`

**Events published:** `ListingCreated`, `OrderMatched`, `TradeSettled`

---

### Tier 2 — Complete rental & compliance loop

#### 4.4 Rental / Property Management Service (`rental-service`, :8086)

**Why:** Rent tokenization requires lease lifecycle, not manual dividend input.

| Responsibility | Details |
|----------------|---------|
| Lease management | Create/update leases; link to `LEASE_AGREEMENT` docs |
| Tenant onboarding | Tenant role, lease terms, rent schedule |
| Rent schedules | Due dates, reminders, late fees |
| Occupancy tracking | Vacancy alerts, turnover |
| Dividend trigger | Auto-compute `annualRentalIncomeUsd`; trigger Issuance distribute |
| Maintenance | Maintenance tickets (optional v2) |

**Key entities:** `Lease`, `Tenant`, `RentPayment`, `MaintenanceTicket`

---

#### 4.5 KYC / Compliance Service (`compliance-service`, :8087)

**Why:** Extract from Token Issuance; compliance will grow (AML, jurisdiction, accredited investor).

| Responsibility | Details |
|----------------|---------|
| Investor KYC | Sumsub, Onfido, or manual review workflow |
| SPV KYC | Unified with registry SPV KYC flag |
| Whitelist sync | Push approved wallets to Token Issuance + on-chain `ComplianceRegistry` |
| Expiry & revocation | Periodic re-verification, audit trail |
| Jurisdiction rules | Country-specific investment restrictions |

**Extract from:** `ComplianceService` in `token-issuance-service`

---

#### 4.6 Document / IPFS Service (`document-service`, :8088)

**Why:** Registry stores `ipfsCid` but never uploads. Legal docs are critical.

| Responsibility | Details |
|----------------|---------|
| Upload to IPFS | Pinata, web3.storage, or self-hosted IPFS node |
| Return CID | Store in Property Registry `PropertyDocument.ipfsCid` |
| Private storage | S3/MinIO for sensitive KYC documents |
| Verification workflow | Integrate with registry document verify endpoint |

---

### Tier 3 — Platform polish

#### 4.7 Wallet Service

- Custodial wallets for non-crypto-native investors
- WalletConnect / MetaMask linking
- Transaction signing for transfers and dividend claims
- Multi-property balance aggregation

#### 4.8 Notification Service

- Email / push / SMS
- Subscribes to Kafka: KYC approved, dividend paid, rent due, transfer confirmed

#### 4.9 API Gateway / BFF (`api-gateway`, :8080) — **MVP implemented**

- Single entry point for web/mobile clients
- Route to backend services
- Aggregate responses (flat + token price + listing in one call) — `GET /v1/bff/flats/{id}`, `/v1/bff/listings/{id}`
- Investor Portal consumes BFF on `:5173` → gateway `:8080`

#### 4.10 Blockchain Indexer (`blockchain-indexer-service`, :8091) — **MVP implemented**

- Listen to on-chain events (Transfer, DividendPaid, WhitelistUpdated)
- Sync state to DB; reconcile with Payment Service
- Replace trust in post-tx local balance updates

---

### Tier 4 — Phase 6: Scale, ops visibility & RWA depth (planned)

Phases 0–5 delivered 12 backend microservices; Phase 6 adds seven more (19 total). Phase 6 adds **read-side, ops, and compliance-depth** services without splitting cohesive write engines (Payment escrow, Issuance deploy/transfer, Compliance limits). See [§12 Phase 6 — Planned Services](#12-phase-6--planned-services) for full descriptions and build order.

| Service | Port | Folder (proposed) | Priority |
|---------|------|-------------------|----------|
| Reporting / Analytics | 8093 | `reporting-service/` | **P1** — implemented |
| Settlement / Saga Tracker | 8094 | `settlement-service/` | **P1** — implemented |
| Valuation / NAV | 8095 | `valuation-service/` | **P2** — implemented |
| Audit Ledger | 8096 | `audit-ledger-service/` | **P2** — implemented |
| Corporate Actions | 8097 | `corporate-actions-service/` | **P2** — implemented |
| Search | 8098 | `search-service/` | **P3** — implemented (PostgreSQL index) |
| Integration Hub | 8099 | `integration-hub-service/` | **P3** — implemented |

**Explicitly not new services:** limits/policy engine (stay in Compliance + Marketplace), config service, Payment escrow split, Issuance core split, Property Registry folder rename (deferred ADR 006).

---

## 5. Domain Flows — Sell & Rent

See diagrams:
- [`diagrams/04-sell-flow.puml`](diagrams/04-sell-flow.puml)
- [`diagrams/05-rent-flow.puml`](diagrams/05-rent-flow.puml)
- [`diagrams/03-token-lifecycle.puml`](diagrams/03-token-lifecycle.puml)

### 5.1 Token lifecycle (as designed)

```
1. Flat registered in Property Registry (status: AVAILABLE)
2. SPV registered + KYC verified
3. POST /v1/tokens → deploy PropertyToken.sol on Polygon
4. Callback PATCH /v1/flats/{id}/token-info → flat TOKENIZED
5. Investors registered + KYC verified
6. PATCH /v1/tokens/{id}/enable-transfers → trading opens
7. Investors buy/sell via Marketplace + Payment + Issuance transfers
8. Monthly: rent collected → dividend distributed to holders
```

### 5.2 Use case → service mapping

| Use case | Services involved |
|----------|-------------------|
| Tokenize flat for sale | Registry → Issuance → Marketplace (listing) |
| Investor buys tokens | Marketplace → Payment (USDC) → Issuance (transfer) → Registry (`FULLY_SOLD`) |
| Tokenize for rental yield | Same tokenization; listing marked "rental yield" |
| Tenant pays rent (crypto) | Rental → Payment → SPV wallet |
| Investors receive dividends | Rental triggers → Issuance (distribute) → Payment (payout) |
| Secondary sale | Marketplace → Compliance → Payment → Issuance (transfer) |

### 5.3 Payment flows

**BUY:**
```
Investor pays USDC → Payment (escrow) → Token Issuance mints/transfers
→ on-chain confirmed → Payment releases escrow → Marketplace marks settled
```

**RENT:**
```
Tenant pays USDC → Payment → SPV wallet → Rental Service confirms
→ Token Issuance POST /dividends/distribute → Payment pays holders pro-rata
```

---

## 6. Event-Driven Integration (Kafka)

See diagram: [`diagrams/06-kafka-events.puml`](diagrams/06-kafka-events.puml)

### 6.1 Topic naming convention

```
tokenrealty.{domain}.{event}
```

Examples: `tokenrealty.registry.flat-tokenized`, `tokenrealty.marketplace.order-matched`

### 6.2 Event catalog

| Event | Publisher | Consumers | Payload (key fields) |
|-------|-----------|-----------|----------------------|
| `BuildingApproved` | Property Registry | Marketplace, Notification | `buildingId`, `approvedAt` |
| `FlatTokenized` | Property Registry | Marketplace, Notification | `flatId`, `contractAddress`, `totalTokens` |
| `InvestorKycApproved` | Compliance Service | Token Issuance | `investorId`, `walletAddress` |
| `InvestorKycRevoked` | Compliance Service | Token Issuance | `investorId`, `walletAddress`, `reason` |
| `ListingCreated` | Marketplace | Notification | `listingId`, `flatId`, `priceUsd` |
| `OrderMatched` | Marketplace | Payment, Token Issuance | `orderId`, `buyerId`, `sellerId`, `tokenAmount` |
| `PaymentConfirmed` | Payment | Token Issuance, Marketplace | `paymentId`, `amount`, `currency`, `txHash` |
| `TransferCompleted` | Token Issuance | Marketplace, Notification, Registry | `transferId`, `contractId`, `from`, `to`, `amount` |
| `RentCollected` | Payment | Rental, Token Issuance | `leaseId`, `amount`, `period` |
| `DividendDistributed` | Token Issuance | Payment, Notification | `contractId`, `totalAmount`, `holderPayouts[]` |
| `DocumentUploaded` | Document Service | Property Registry | `documentId`, `ipfsCid`, `documentType` |

### 6.3 Kafka setup TODO

- [x] Kafka (KRaft) in `docker-compose.yml` — `docker compose --profile kafka up -d` from repo root
- [x] Define shared event schema (JSON Schema in `docs/schemas/` — settlement-flow payloads)
- [x] Create `tokenrealty-events` shared library (Java records + serializers)
- [x] Add `@KafkaListener` stubs in each service as they are built

---

## 7. Service Port & Naming Convention

| Service | Port | Suggested folder | Database |
|---------|------|------------------|----------|
| API Gateway | 8080 | `api-gateway/` | — |
| Property Registry | 8081 | `property-registry-service/` *(rename from `token-realty-app`)* | `property_registry` |
| Token Issuance | 8082 | `token-issuance-service/` | `token_issuance` |
| Auth | 8083 | `auth-service/` | `auth_service` |
| Marketplace | 8084 | `marketplace-service/` | `marketplace_service` |
| Payment | 8085 | `payment-service/` | `payment_service` |
| Rental / Property Mgmt | 8086 | `rental-service/` | `rental_service` |
| KYC / Compliance | 8087 | `compliance-service/` | `compliance_service` |
| Document / IPFS | 8088 | `document-service/` | `document_service` |
| Notification | 8089 | `notification-service/` | `notification_service` |
| Wallet | 8090 | `wallet-service/` | `wallet_service` |
| Blockchain Indexer | 8091 | `blockchain-indexer-service/` | `blockchain_indexer` |

**Phase 6 (planned — not yet scaffolded):**

| Service | Port | Suggested folder | Database |
|---------|------|------------------|----------|
| Reporting / Analytics | 8093 | `reporting-service/` | `reporting_service` |
| Settlement / Saga Tracker | 8094 | `settlement-service/` | `settlement_service` |
| Valuation / NAV | 8095 | `valuation-service/` | `valuation_service` |
| Audit Ledger | 8096 | `audit-ledger-service/` | `audit_ledger_service` |
| Corporate Actions | 8097 | `corporate-actions-service/` | `corporate_actions_service` |
| Search | 8098 | `search-service/` | — (OpenSearch/Elasticsearch index) |
| Integration Hub | 8099 | `integration-hub-service/` | `integration_hub_service` |

**Infrastructure (non-HTTP):**

| Component | Port |
|-----------|------|
| PostgreSQL | 5432 |
| Kafka | 9092 |
| Apicurio Schema Registry | 8092 |
| Redis | 6379 |
| Hardhat node | 8545 |
| IPFS API | 5001 |
| pgAdmin | 5050 |

---

## 8. Implementation Phases

See diagram: [`diagrams/07-build-phases.puml`](diagrams/07-build-phases.puml)

| Phase | Focus | Outcome |
|-------|-------|---------|
| **Phase 0** | Foundation fixes | Blockchain works; inter-service auth; Liquibase enabled |
| **Phase 1** | Platform layer | Auth Service, Kafka, API Gateway, root README |
| **Phase 2** | Commerce | Payment + Marketplace — buy tokens with crypto |
| **Phase 3** | Rental | Rental Service + rent → dividend pipeline |
| **Phase 4** | Compliance & docs | KYC Service, Document/IPFS Service |
| **Phase 5** | Scale & UX | Wallet, Notification, Blockchain Indexer, frontend |
| **Phase 6** | Ops visibility & RWA depth | Reporting, Settlement Saga, Valuation/NAV, Audit Ledger, Corporate Actions; Search + Integration Hub |
| **Phase 7** | Event mesh & production integrations | Wire Phase 6 publishers to consumers; Hub payment webhooks; OpenSearch (optional) |

See diagram: [`diagrams/07-build-phases.puml`](diagrams/07-build-phases.puml).

**Recommended Phase 6 build order:** Reporting → Settlement Saga → Valuation/NAV → Audit Ledger → Corporate Actions → Search → Integration Hub.

---

## 9. Foundation Fixes (Phase 0)

> **Must complete before new microservices.** Without these, the platform cannot operate end-to-end.

### 9.1 Smart contracts

- [x] Implement `PropertyToken.sol` (ERC-1400 or ERC-20 + compliance hook)
- [x] Implement `ComplianceRegistry.sol` (whitelist add/remove/check)
- [x] Implement `DividendDistributor.sol` (optional; or dividend logic in PropertyToken)
- [x] Fix file naming: `Propertytoken.sol` → `PropertyToken.sol`, `Complianceregistry.sol` → `ComplianceRegistry.sol`
- [x] Create `hardhat/scripts/deployFlat.js` (called by `ContractDeployer`)
- [x] Run `npm run compile` and verify artifacts generated
- [x] Test deploy on local Hardhat node (`hardhat/scripts/deploy.js` → `deployments/localhost.json`)
- [x] Copy contract addresses to profiles (`export-env.sh`, `local` Spring profiles — see [hardhat-demo.md](hardhat-demo.md))

### 9.2 Inter-service authentication

- [x] Add service account credentials to Auth Service (`DevDataInitializer`)
- [x] Configure `PropertyRegistryClient` / `TokenIssuanceClient` RestClient with Bearer service token
- [x] Test Issuance → Registry `PATCH /v1/flats/{id}/token-info` callback end-to-end (`PropertyRegistryClientIntegrationTest`)
- [x] Document service-to-service auth pattern (`tokenrealty-security` + `ServiceTokenProvider`)

### 9.3 Database schema management

- [x] Enable Liquibase in Property Registry (`application-prod.yml`)
- [x] Create Liquibase migrations for Token Issuance (`db/changelog/`)
- [x] Disable Hibernate `ddl-auto: update` in production profiles (`ddl-auto: validate`)
- [x] Add service databases to docker-compose init (`docker/postgres/init-databases.sql`)

### 9.4 Dividend implementation

- [x] Dividend calculation in Issuance → `dividend.distributed` → Payment on-chain USDC payouts (when `PAYMENT_BLOCKCHAIN_ENABLED=true`)
- [x] Replace simulated dividend `txHash` in Issuance DB with Payment `payout.completed` callback
- [x] Implement `@Scheduled` monthly dividend job (Rental Service rent summary)
- [x] Replace hardcoded `MATIC_USD_RATE = 0.85` with price oracle or config — N/A (USDC-only payouts; MATIC rate removed from scope)

### 9.5 Business logic gaps

- [x] Implement `FULLY_SOLD` flat status when all tokens distributed (Marketplace → Registry on primary sell-out)
- [x] Primary token transfer via `operatorTransfer` (custodial operator signs for SPV wallet)
- [x] Hardhat contract tests (`hardhat/test/` — PropertyToken, ComplianceRegistry, MockUSDC)
- [x] Web3j wrapper generation script (`npm run generate-wrappers`)
- [x] Migrate `BlockchainConnector` to Web3j `FunctionEncoder` helpers (`*Encoder` classes; optional `npm run generate-wrappers` for full wrappers)

### 9.6 Documentation

- [x] Tier-1 business invariants doc ([docs/BUSINESS_RULES.md](BUSINESS_RULES.md) + `.cursor/rules/business-rules.mdc`)
- [x] Create root `README.md` with platform overview and startup order
- [x] Sync API tables in service READMEs with actual controllers (see [rules/service-readmes.md](rules/service-readmes.md))
- [x] Update Property Registry "Next Steps" (Issuance token-info callback + Kafka + JWT done)

---

## 10. Per-Service TODO Checklists

### 10.1 Auth Service (Phase 1)

- [x] Scaffold Spring Boot 4 / Java 21 project (`auth-service/`)
- [x] User entity: id, email, passwordHash, role, walletAddress, createdAt
- [x] RefreshToken + ServiceAccount entities
- [x] JWT access token (15 min) + refresh token (7 days) via jjwt
- [x] POST `/v1/auth/register`, POST `/v1/auth/login`, POST `/v1/auth/refresh`
- [x] POST `/v1/auth/service-token` for inter-service JWT
- [x] GET `/v1/users/me`, PATCH `/v1/users/me/wallet`
- [x] Dev seed users + service accounts (`DevDataInitializer`)
- [x] Unit + context tests (`./mvnw test`)
- [x] Shared `tokenrealty-security` library for other services (JWT validation filter)
- [x] Migrate registry/issuance/marketplace from Basic auth to JWT resource server
- [x] Wire `PropertyRegistryClient` and `TokenIssuanceClient` with Bearer service token

### 10.2 API Gateway (Phase 1)

- [x] Scaffold Spring Cloud Gateway or similar (`api-gateway/`)
- [x] Route definitions for all services (incl. notification, document)
- [x] JWT validation at gateway level
- [x] CORS configuration
- [x] Rate limiting (optional — in-memory per-IP at gateway)
- [x] Health check aggregation endpoint (`GET /actuator/platform-health`)

### 10.3 Kafka infrastructure (Phase 1)

- [x] Root `docker-compose.yml` with Kafka profile (Schema Registry optional — not required for JSON events)
- [x] Shared event library (`tokenrealty-events/`)
- [x] Define settlement-flow event schemas (see `docs/schemas/`; full catalog in Section 6.2 / EVENTS.md)
- [x] Add Kafka producers to Property Registry (BuildingApproved, FlatTokenized)
- [x] Add Kafka consumers in Notification Service (email + preference-aware delivery)

### 10.4 Marketplace Service (Phase 2)

- [x] Scaffold project (`marketplace-service/`)
- [x] Entities: Listing, MarketOrder, Trade, OutboxEvent
- [x] POST `/v1/listings` — create listing for tokenized flat
- [x] GET `/v1/listings` — search/filter listings
- [x] POST `/v1/orders` — place buy order (primary market)
- [x] Simple order match on buy (reserve tokens, status MATCHED)
- [x] KYC check via Compliance Service `GET /v1/compliance/check/{wallet}` (`ComplianceClient`)
- [x] Investment policy gate via `POST /v1/compliance/check-investment` before order match
- [x] Outbox publisher for `listing.created`, `order.matched`, `trade.settled`
- [x] Admin PATCH `/v1/orders/{id}/settle` (fallback; automated flow via Kafka when enabled)
- [x] Unit + context tests (`./mvnw test`)
- [x] Secondary market sell orders (`POST /v1/listings/secondary`, `POST /v1/orders/sell`)
- [x] Kafka relay (outbox → broker via `OutboxRelayWorker`)
- [x] Consumer: auto-create listing on `flat.tokenized` (requires `building.approved` gate)
- [x] Integrate with Payment Service (escrow on match via `PaymentClient`)
- [x] Integrate with Token Issuance (transfer on payment confirmed via Kafka)

### 10.5 Payment Service (Phase 2)

- [x] Scaffold project (`payment-service/`)
- [x] Entities: Payment, Escrow, Payout, WalletBalance, LedgerEntry
- [x] POST `/v1/payments` — initiate crypto payment (Idempotency-Key)
- [x] POST `/v1/payments/{id}/confirm` — webhook/callback on chain confirmation
- [x] Escrow hold/release/refund logic
- [x] POST `/v1/payouts` — dividend/rent payout to holder wallets
- [x] Web3j integration for USDC payouts (`PaymentBlockchainService`; MockUSDC on local Hardhat; simulated when disabled)
- [x] Transaction ledger entries (double-entry stub)
- [x] On-chain reconciliation job
- [x] Publish `PaymentConfirmed`, `RentCollected` events (outbox stub)
- [x] Unit + context tests (`./mvnw test`)
- [x] Integrate Marketplace order match → payment initiate

### 10.6 Rental Service (Phase 3)

- [x] Scaffold project (`rental-service/`)
- [x] Entities: Lease, RentPayment (Tenant, MaintenanceTicket deferred)
- [x] POST `/v1/leases` — create lease for flat
- [x] GET `/v1/leases/{id}` — lease details
- [x] Rent schedule generation (monthly due dates)
- [x] POST `/v1/rent-payments` — record rent payment → Payment `POST /v1/payouts` (RENT)
- [x] Auto-trigger dividend distribution via Kafka (`rent.collected` → Issuance → `dividend.distributed` → Payment)
- [x] GET `/v1/occupancy/flats/{flatId}` — occupancy status
- [x] Publish `RentDue`, `LeaseExpired` events
- [x] Unit + context tests (`./mvnw test`)

### 10.7 Compliance Service (Phase 4)

- [x] Extract compliance logic from Token Issuance
- [x] Scaffold project (`compliance-service/`, port 8087)
- [x] Entity: `ComplianceRecord` (register / verify / revoke)
- [x] POST `/v1/compliance` — register investor for KYC
- [x] PATCH `/v1/compliance/{id}/verify` and `/revoke` — approve/reject
- [x] Webhook endpoint for Sumsub/Onfido (optional) — `POST /v1/compliance/webhooks/kyc/{provider}`
- [x] Sync on-chain whitelist via Kafka → Issuance `KycApprovedListener` / `KycRevokedListener`
- [x] Periodic expiry check (@Scheduled)
- [x] Publish `kyc-approved`, `kyc-revoked` events (outbox)
- [x] Unit + context tests

### 10.8b Property Registry — Asset Metadata Expansion (Phase 4b)

Blueprint for legal, physical, and financial metadata required for tokenized real estate (adapted from external registry spec). **Do not add per-document `*IpfsCid` columns** — use existing `PropertyDocument` + `DocumentType` enum; Document Service uploads and sets `ipfsCid`.

#### Phase 4b-1 — Implemented (Property Registry entities)

| Domain | Field | Entity | Notes |
|--------|-------|--------|-------|
| Legal / SPV | `legalJurisdiction` | `SpvEntity` | State/province (e.g. Delaware, DIFC) |
| Legal / SPV | `ownershipType` | `SpvEntity` | `DIRECT_DEED`, `SPV_SHARE_EQUITY`, `PART_DEBT_INSTRUMENT`, `PROFIT_SHARING_AGREEMENT` |
| Legal / SPV | `registrationNumber`, `registrationCountry` | `SpvEntity` | Already existed |
| Physical | `propertyCategory` | `Building` | `RESIDENTIAL_FLAT`, `COMMERCIAL_BUILDING`, … |
| Physical | `constructionYear` | `Building` | Already existed |
| Physical | `cadastralReference` | `Building`, `Flat` | Land registry / title deed parcel ID |
| Physical | `areaSqm`, `netUsableAreaSqm` | `Flat` | Gross vs net usable area |
| Valuation | `valueUsd`, `valuationDate`, `appraiserName` | `Valuation` | Already existed |
| Valuation | `operatingExpensesEstimateUsd` | `Valuation` | Annual opex baseline |
| Valuation | `targetRentalYieldPct` | `Valuation` | Projected net yield % |
| Documents | typed data room | `PropertyDocument` | `TITLE_DEED`, `VALUATION_REPORT`, `ARTICLES_OF_INCORPORATION`, `ENVIRONMENTAL_AUDIT`, … |

#### Phase 4b-2 — With Document Service (:8088)

- [x] `POST /v1/documents/upload` → IPFS → register `PropertyDocument` with CID
- [x] Verify workflow links documents to building/flat/SPV (Compliance review queue + Registry verify)
- [x] Optional S3/MinIO for private KYC/legal docs (`storageUrl`) — Document Service routes `KYC_DOCUMENT` / `INSURANCE_POLICY` to MinIO

#### Phase 4b-3 — Deferred

- [x] `energyEfficiencyRating`, `zoningCode` on `Building`
- [x] `lastRenovationYear` on `Building`
- [x] Non-equity token structures (`PART_DEBT_INSTRUMENT`, `PROFIT_SHARING_AGREEMENT`) — PRIMARY listings with `instrumentType`; secondary blocked (tracks 153)

### 10.8 Document Service (Phase 4)

- [x] Scaffold project (`document-service/`)
- [x] POST `/v1/documents/upload` — multipart upload → IPFS
- [x] GET `/v1/documents/{documentId}` — retrieve metadata from Registry
- [x] IPFS client (simulated dev mode + optional Pinata API)
- [x] Optional S3/MinIO for private documents (`storageUrl` on KYC / insurance uploads)
- [x] Callback to Property Registry with CID
- [x] Publish `DocumentUploaded` event (outbox)
- [x] Unit + context tests

### 10.9 Notification Service (Phase 5)

- [x] Scaffold project (`notification-service/`)
- [x] Email provider integration (log mode dev + optional SMTP)
- [x] Kafka consumers for key notification events (KYC, trade settled, dividend, rent)
- [x] Simple email templates per event type
- [x] POST `/v1/notifications/send` — manual trigger (admin)
- [x] Notification preferences per user (JPA GET/PATCH `/v1/notifications/preferences/{userId}`)
- [x] Kafka integration test — `trade.settled` ingest + preference gate + eventId dedupe

### 10.10 Wallet Service (Phase 5)

- [x] Scaffold project (`wallet-service/`, port 8090)
- [x] Custodial wallet creation (generate keypair, encrypt at rest)
- [x] WalletConnect / MetaMask linking for non-custodial (`POST /v1/wallets/link`)
- [x] GET `/v1/wallets/{investorId}/balance` — aggregate balances
- [x] POST `/v1/wallets/{investorId}/sign` — sign transaction (custodial)
- [x] Integration with Payment Service for payouts (`GET /v1/wallet-balances/{investorId}`)

### 10.11 Blockchain Indexer (Phase 5)

- [x] Standalone service (`blockchain-indexer-service/`, port 8091)
- [x] Subscribe to Polygon/Hardhat node for contract events
- [x] Sync Transfer, WhitelistAdded, WhitelistRemoved to DB
- [x] Reconciliation job: on-chain balance vs DB holder balance
- [x] Alert on mismatch (logged + `GET /v1/indexer/reconciliation`)

### 10.12 Reporting / Analytics Service (Phase 6)

- [x] Scaffold project (`reporting-service/`, port 8093)
- [x] Kafka consumers: `trade.settled`, `dividend.distributed`, `rent.collected`, `order.matched`, `flat.tokenized`
- [x] Materialized read models: trading volume, occupancy, dividend aggregates
- [x] GET `/v1/reports/trading-summary`, `/v1/reports/occupancy`, `/v1/reports/dividends`
- [x] Regulatory export: transaction history (JSON via `/v1/reports/export`)
- [x] Gateway BFF aggregate `GET /v1/bff/admin/reports/summary`
- [x] Kafka integration tests; README + docs/README.md

### 10.13 Settlement / Saga Tracker Service (Phase 6)

- [x] Scaffold project (`settlement-service/`, port 8094)
- [x] Saga state machine: primary/secondary buy flow steps (match → payment → transfer → settled)
- [x] Kafka consumers: `order.matched`, `payment.confirmed`, `transfer.completed`, `trade.settled`
- [x] GET `/v1/settlements/{orderId}` — step timeline + current status
- [x] Admin retry hook `POST /v1/settlements/{orderId}/retry` (STUCK → IN_PROGRESS)
- [x] Scheduled stuck detection (`tokenrealty.settlement.stuck-sla-minutes`)
- [x] Outbox publish `settlement.stuck`, `settlement.recovered`
- [x] Kafka integration tests

### 10.14 Valuation / NAV Service (Phase 6 — implemented)

- [x] Scaffold project (`valuation-service/`, port 8095)
- [x] Appraisal workflow for `APPRAISER` role (submit, review, approve/reject)
- [x] Periodic revaluation schedules per building/flat
- [x] Token NAV calculation from latest valuation + outstanding tokens
- [x] Outbox publish `valuation.updated` (Registry sync via future consumer)
- [x] GET `/v1/valuations/building/{id}`, `/v1/valuations/flat/{id}/nav`
- [x] Unit + integration tests

### 10.15 Audit Ledger Service (Phase 6 — implemented)

- [x] Scaffold project (`audit-ledger-service/`, port 8096)
- [x] Append-only audit entries (no updates/deletes)
- [x] Kafka consumers: KYC approve/revoke, trade.settled, document.uploaded, order.matched
- [x] GET `/v1/audit/investor/{id}`, `/v1/audit/flat/{id}` — paginated immutable trail
- [x] Regulatory export endpoint `GET /v1/audit/export`
- [x] Unit + Kafka integration tests

### 10.16 Corporate Actions Service (Phase 6 — implemented)

- [x] Scaffold project (`corporate-actions-service/`, port 8097)
- [x] Extract dividend distribution orchestration from Issuance (rent.collected → distribution-requested)
- [x] Issuance keeps deploy/transfer/on-chain whitelist; listens to `dividend.distribution-requested`
- [x] Kafka: consume `rent.collected`, publish `dividend.distribution-requested` via outbox
- [x] Future: stock splits, rights issues (schema only in v1)
- [x] GET `/v1/corporate-actions/dividends`, `/v1/corporate-actions/{actionId}`
- [x] Unit + integration tests

### 10.17 Search Service (Phase 6 — implemented, P3)

- [x] Scaffold project (`search-service/`, port 8098)
- [x] PostgreSQL search index for listings and buildings (OpenSearch deferred)
- [x] Kafka consumers: `listing.created`, `flat.tokenized`, `building.approved`, `valuation.updated`
- [x] GET `/v1/search/listings?q=`, `/v1/search/buildings?q=` with filters + pagination
- [x] Gateway route
- [x] Optional BFF wrapper (`GET /v1/bff/search/listings`, `/buildings`)

### 10.18 Integration Hub Service (Phase 6 — implemented, P3)

- [x] Scaffold project (`integration-hub-service/`, port 8099)
- [x] Inbound KYC webhook relay → Compliance with signature header passthrough
- [x] Delivery tracking with retry (max 5 attempts, exponential backoff)
- [x] GET `/v1/integrations/deliveries` (ADMIN)
- [x] Gateway route `/api/v1/integrations`
- [x] Document storage webhook relay (`POST /v1/integrations/webhooks/storage/{provider}`)
- [x] Payment webhook relay (`POST /v1/integrations/webhooks/payment/{provider}`)
- [x] Credential rotation via KMS/local encryption (`POST /v1/integrations/credentials/{type}/{provider}/rotate`)

---

## 11. Cross-Cutting Concerns

### 11.1 Shared libraries (recommended)

| Library | Contents |
|---------|----------|
| `tokenrealty-events` | Kafka event records, serializers, topic constants |
| `tokenrealty-security` | JWT filter, role annotations, service account client |
| `tokenrealty-common` | BaseEntity, pagination, exception types, API error format |

### 11.2 Docker / local dev

- [x] Root `docker-compose.yml`: PostgreSQL (all DBs), Kafka profile
- [x] Profile-based startup: `docker compose up postgres` / `--profile kafka`
- [x] Seed data script for demo buildings + flats + test users + tenant lease (`scripts/seed-demo.sh`, `scripts/demo-services.sh` includes rental + notification)
- [x] One-command demo script (`scripts/demo-all.sh`) — infra → services → wait → seed [→ E2E]
- [x] Dev seed: pending document review for admin E2E (`DevComplianceDocumentReviewInitializer` + registry demo document)

### 11.3 Observability

- [x] Structured logging (JSON) with `traceId` via `tokenrealty-web` (`TraceIdFilter`, logback-spring.xml)
- [x] Micrometer + Prometheus metrics (`/actuator/prometheus` on all services)
- [x] Prometheus alert rules (service down, indexer balance mismatch — `docker/observability/prometheus/alerts/`)
- [x] Distributed tracing (OpenTelemetry OTLP export; Jaeger in compose `otel` profile)
- [x] Health checks: `/actuator/health` on all services

### 11.4 CI/CD

- [x] GitHub Actions: Java tests (all services), Hardhat tests, frontend builds, OpenAPI codegen check
- [x] Contract compile + test in CI (Hardhat job)
- [x] Docker image build per service (generic `docker/Dockerfile.spring-service` + CI matrix: gateway, auth, registry, marketplace, payment)
- [x] Integration test suite with Testcontainers (PostgreSQL — marketplace buy-flow, payment escrow)
- [x] Property Registry H2 integration test in CI (`PropertyRegistryIntegrationTest`)
- [x] Kafka ingest ITs: buy/rent/KYC/dividend/payment.confirmed paths + notification preference gates (`kafka-integration-tests` CI job)
- [x] Local Kafka DLQ enabled in `application-local.yml` for consuming services
- [x] Docker CI matrix covers all 12 HTTP services
- [x] Kafka ITs: buy-flow settlement, payout.completed, notification kyc/dividend/flat/order paths
- [x] Service ITs: rental rent payment, wallet aggregate balance, indexer reconciliation
- [x] Demo identity alignment (auth investor id/wallet ↔ compliance/payment)
- [x] Dev seeds: linked wallet, simulated token contract, `seed-tokenize-demo.sh`
- [x] E2E: investor dividend history + admin building detail specs
- [x] Payment custodial balance guard on escrow initiate
- [x] Shared Kafka DLQ handler (`tokenrealty-kafka`, opt-in)
- [x] Gateway Redis-backed rate limiting (compose Redis :6379)
- [x] Liquibase migrations: marketplace, compliance, rental (tracks 114–116)
- [x] Marketplace `building.approved` Kafka consumer + approved-building gate on auto-listing
- [x] OpenAPI specs + codegen for payment, wallet, issuance, notification, compliance (tracks 103–107)
- [x] E2E full specs: buy-settled, notification-prefs, admin-tokenize, dividend-history (tracks 108–110)
- [x] `demo-all.sh --buy` one-command buy-flow demo (track 111)
- [x] Indexer balance remediation, dividend event indexing, WebSocket log subscriber (tracks 118–120)
- [x] WalletConnect session API + KMS prod encryption config (tracks 121–122)
- [x] Kafka/outbox CI: `OutboxRelayIntegrationTest`, `DocumentUploadedOutboxIntegrationTest`, `KafkaDlqIntegrationTest`
- [x] Kafka ITs: `BuildingApprovedKafkaIntegrationTest`, `RentScheduleKafkaIntegrationTest`, `TransferCompletedKafkaIntegrationTest`
- [x] Service ITs: `BalanceRemediationIntegrationTest` (indexer remediation)
- [x] `BuyFlowKafkaContainersIntegrationTest` (Testcontainers Kafka buy-flow)
- [x] Liquibase prod-profile CI validation: payment, registry, issuance (tracks 154–156)
- [x] Payment escrow deposit address watcher via `eth_getLogs` (track 167)
- [x] Secondary sell orders create `PENDING` listing intent (track 168)
- [x] OpenAPI indexer + maintenance tickets; shared-api-types wiring (tracks 158, 171)
- [x] Tenant/admin maintenance UI + gateway BFF (tracks 165, 172, 173)
- [x] Grafana alert + indexer mismatch panels (track 166)
- [x] Notification readable email templates (track 169)
- [x] CI: `MaintenanceTicketIntegrationTest`, `BuyFlowIntegrationTest`, `DataRoomLoopIntegrationTest` (track 170)
- [x] DividendDistributor on-chain deposit when configured (track 159)
- [x] WalletConnect session with relay URL + TTL; key rotation endpoint (tracks 160, 176)
- [x] Investor listing PRIMARY/SECONDARY filters (track 177)
- [x] Full compose stack E2E on pull_request (`compose-e2e-pr` CI job + `scripts/ci-compose-e2e.sh`)
- [x] GraphQL BFF layer (`POST /graphql` aggregating REST BFF queries — track 163)
- [x] Avro Schema Registry (Apicurio in compose + `AvroEventCodec` — track 164)
- [x] Real AWS/GCP KMS wallet encryption adapters (track 161)
- [x] Compose E2E subset expanded to 5 specs: login, portfolio, KYC, tenant/admin maintenance (track 178)
- [x] Avro opt-in: `AvroSchemaRegistrar`, `KafkaSerializationProperties`, `AvroOutboxPayloadEncoder` + marketplace relay hook (tracks 179, 198)
- [x] Secondary buy matches pending sell order + escrow on match (tracks 180, 192)
- [x] Liquibase migration IT: auth-service (track 181)
- [x] Liquibase migration IT: document-service (track 182)
- [x] Liquibase migration IT: notification-service (track 183)
- [x] Liquibase migration IT: wallet-service (track 184)
- [x] Liquibase migration IT: blockchain-indexer-service (track 185)
- [x] Liquibase migration IT: compliance-service (track 186)
- [x] Liquibase migration IT: marketplace-service (track 187)
- [x] Liquibase migration IT: rental-service (track 188)
- [x] E2E: tenant maintenance ticket create + list verify (track 189)
- [x] E2E: admin maintenance queue load + resolve open ticket (track 190)
- [x] Investor portal WalletConnect connector via `VITE_WALLETCONNECT_PROJECT_ID` (track 191)
- [x] Sumsub webhook HMAC verification (`KycWebhookSignatureVerifier`, raw body + digest headers — track 193)
- [x] Sumsub outbound applicant creation wired on compliance register (track 194)
- [x] Pinata IPFS prod path documented in compose + document-service prod config (track 195)
- [x] Indexer reconciliation counters: runs, mismatches, remediated (track 196)
- [x] E2E: investor secondary buy until SETTLED — skips when no secondary listings (track 197)
- [x] Payment secondary escrow release credits seller custodial balance (track 199)
- [x] `OrderServiceTest.placeBuyOrderOnSecondaryMatchesPendingSellOrder` (track 200)
- [x] Nightly compose E2E workflow (`.github/workflows/nightly-e2e.yml`, track 201)
- [x] Tracks 178–201 synced in PLATFORM-SPEC §11.4 (track 202)
- [x] Compose E2E expanded to 11 specs: dividend, notification-prefs, secondary buy, building detail, document review, tenant rent (track 203)
- [x] GraphQL `buildingDetail` + `adminMaintenanceQueue` queries; GraphiQL in local profile (tracks 204–206)
- [x] Payment OpenAPI `sellerRecipientId`; integration test for seller credit on escrow release (tracks 207–208)
- [x] Onfido KYC webhook provider + `OnfidoProperties` config (tracks 209, 223)
- [x] Self-hosted Kubo IPFS pinning mode (`IPFS_MODE=kubo`, track 210)
- [x] Payment outbox Avro serialization hook (`tokenrealty.kafka.serialization=avro`, track 211)
- [x] Onfido webhook integration test (track 212)
- [x] Investor portal GraphQL portfolio client (`VITE_USE_GRAPHQL_BFF`, track 213)
- [x] Wallet encryption round-trip integration test (track 214)
- [x] `IpfsStorageServiceTest` simulated + kubo failure paths (track 215)
- [x] ADR 001 ERC-1400 token standard decision (track 216)
- [x] ADR 002 Kafka Avro opt-in decision (track 217)
- [x] EVENTS.md Avro serialization section (track 218)
- [x] Gateway BFF `GET /v1/bff/admin/maintenance-tickets` (track 220)
- [x] GraphQL controller tests for building + admin maintenance (track 221)
- [x] Prometheus alert documents Micrometer → Prometheus metric mapping (track 226)
- [x] Tracks 203–226 synced in PLATFORM-SPEC §11.4 (track 227)
- [x] Compose E2E expanded to all 16 full specs + optional Hardhat profile (`CI_E2E_HARDHAT`, track 228)
- [x] Nightly E2E workflow runs with `CI_E2E_HARDHAT=true` (track 229)
- [x] `e2e-run.sh --compose-subset` mirrors CI compose spec list (track 242)
- [x] Fix KMS stub wallet encryption without requiring local-mode bean (track 230)
- [x] `OutboxRelay.resolvePayloadTransform` shared Avro helper in tokenrealty-outbox (track 231)
- [x] Avro opt-in outbox relay on compliance, document, rental, issuance, registry, indexer (tracks 232–237)
- [x] Marketplace + payment outbox relays refactored to shared `resolvePayloadTransform` (tracks 238–239)
- [x] Onfido outbound applicant client wired on compliance register (track 240)
- [x] Compliance `OutboxRelayIntegrationTest` (track 249)
- [x] KMS stub wallet encryption integration test (track 250)
- [x] Gateway OpenAPI admin maintenance BFF path (track 241)
- [x] ADRs 003–006: USDC, hybrid wallet, servlet gateway, monorepo (tracks 243–246)
- [x] `OutboxRelayAvroTransformTest` in tokenrealty-outbox (track 247)
- [x] Demo stack GraphiQL URL hint in `demo-all.sh` (track 248)
- [x] Gateway OpenAPI codegen for admin maintenance BFF path (track 251)
- [x] Tracks 228–251 synced in PLATFORM-SPEC §11.4 (track 252)
- [x] Shared `OutboxKafkaListenerTestConfiguration` in tokenrealty-kafka (track 253)
- [x] Outbox relay ITs: payment, issuance, registry, rental, indexer, document (tracks 254–260)
- [x] CI `kafka-outbox-tests` covers all eight publishing services (track 261)
- [x] Kafka DLQ config keys on document, rental, indexer, registry, notification (tracks 262–266)
- [x] Payment/issuance/registry test profiles: Kafka bootstrap for outbox ITs (track 267)
- [x] Provider-aware Onfido + Sumsub webhook HMAC verification (tracks 268–269)
- [x] Admin dashboard BFF maintenance path + optional GraphQL client (tracks 270–271)
- [x] `OutboxRelayTest` unit test in tokenrealty-outbox (track 272)
- [x] ADRs 007–008: distributed tracing, E2E testing strategy (tracks 273–274)
- [x] EVENTS.md outbox relay coverage table (track 275)
- [x] `e2e-run.sh --hardhat` + demo-all compose/hardhat hints (tracks 276–277)
- [x] Tracks 253–276 synced in PLATFORM-SPEC §11.4 (track 277)

### 11.5 Frontend

- [x] Investor portal MVP (`frontend/investor-portal/`, `:5173`) — login, listings, BFF detail, buy order, portfolio, order status polling
- [x] Admin dashboard MVP (`frontend/admin-dashboard/`, `:5174`) — login, buildings list, KYC verify
- [x] Tenant portal MVP (`frontend/tenant-portal/`, `:5175`) — login, lease view, pay rent
- [x] openapi-typescript codegen (`frontend/openapi/` → `frontend/shared-api-types/`)
- [x] Shared API client (`frontend/shared-api-client/`) — wired into all three portals
- [x] Admin building detail BFF + flat CRUD UI + order monitoring
- [x] Investor portfolio BFF + dividend history page + JWT refresh rotation
- [x] Playwright E2E smoke suite (`frontend/e2e/`) in CI

See [docs/rules/investor-portal.md](rules/investor-portal.md), [docs/rules/admin-dashboard.md](rules/admin-dashboard.md), [docs/rules/api-gateway-bff.md](rules/api-gateway-bff.md), [docs/rules/e2e-testing.md](rules/e2e-testing.md).

---

## 12. Phase 6 — Planned Services

Phases 0–5 are **complete** (all checkboxes through track 277). Phase 6 extends the platform with **read-side analytics, settlement visibility, RWA compliance depth, and scale integrations** — without violating tier-1 invariants ([BUSINESS_RULES.md](BUSINESS_RULES.md)).

### 12.1 Design principles

| Principle | Rationale |
|-----------|-----------|
| **Do not split write engines** | Payment escrow, Issuance deploy/transfer, and Compliance limits stay in existing services |
| **Event-first read models** | New services consume Kafka; avoid cross-DB joins at query time |
| **Append-only where regulated** | Audit Ledger is immutable; corrections = compensating entries (same pattern as Payment ledger) |
| **Saga tracker ≠ orchestrator rewrite** | Settlement service tracks state and ops recovery; Marketplace/Payment/Issuance still own domain writes |
| **Template** | Scaffold from `marketplace-service/` (layered + kafka/outbox) |

### 12.2 Tier 1 — Ops visibility (build first)

#### 12.2.1 Reporting / Analytics Service (`reporting-service`, :8093)

**Why:** Admin dashboards and regulatory reporting need cross-service aggregates. Today the gateway BFF performs point-in-time REST fan-out; at scale this becomes slow and inconsistent.

| Responsibility | Details |
|----------------|---------|
| Event ingestion | Consume settlement, rental, dividend, and tokenization events |
| Read models | Trading volume, occupancy rates, dividend history aggregates, SPV P&L snapshots |
| Admin APIs | Summary endpoints for admin dashboard charts and exports |
| Regulatory export | Investor holdings + transaction history (CSV/JSON) for compliance filings |
| Idempotency | Dedupe by `eventId` via `KafkaEventConsumer` + `processed_events` |

**Kafka consumes:** `trade.settled`, `dividend.distributed`, `rent.collected`, `order.matched`, `flat.tokenized`, `building.approved`

**Kafka publishes:** none required in v1 (read-only projection)

**Integrates with:** API Gateway BFF (admin charts), Compliance (export requests)

**Key entities:** `TradingSummary`, `OccupancySnapshot`, `DividendAggregate`, `SpvPnlSnapshot`

---

#### 12.2.2 Settlement / Saga Tracker Service (`settlement-service`, :8094)

**Why:** Primary and secondary buy flows span Marketplace → Payment → Issuance → Registry via Kafka. Operators need a single view of in-flight settlements, stuck steps, and retry/compensation — without moving escrow logic out of Payment.

| Responsibility | Details |
|----------------|---------|
| Saga state | Track step progression: match → escrow → payment.confirmed → transfer → transfer.completed → release → trade.settled |
| Visibility | Timeline API per order/trade for admin dashboard |
| Stuck detection | Alert when a step exceeds SLA (configurable per step) |
| Ops recovery | Admin-triggered retry hooks; compensating actions where business rules allow |
| Leave-alone | Escrow hold/release, ledger entries, and token transfer execution remain in Payment/Issuance |

**Kafka consumes:** `order.matched`, `payment.confirmed`, `transfer.completed`, `trade.settled`, `payout.completed`

**Kafka publishes:** `settlement.stuck`, `settlement.recovered` (ops/notification)

**Integrates with:** Marketplace (orderId), Payment (paymentId), Issuance (transferId), Notification (alerts)

**Key entities:** `SettlementSaga`, `SagaStep`, `SagaCompensation`

---

### 12.3 Tier 2 — RWA compliance & trust

#### 12.3.1 Valuation / NAV Service (`valuation-service`, :8095)

**Why:** Property Registry stores `Valuation` metadata, but appraisal workflows, periodic revaluations, and token NAV calculation deserve a dedicated bounded context — especially for the `APPRAISER` role.

| Responsibility | Details |
|----------------|---------|
| Appraisal workflow | Submit → review → approve/reject with appraiser attribution |
| Periodic revaluation | Scheduled re-appraisal per building/flat |
| Token NAV | Net asset value per token from latest approved valuation ÷ outstanding tokens |
| Registry sync | Publish `valuation.updated`; Registry remains source of truth for flat/building links |
| Document link | Reference valuation reports in Document Service / IPFS CIDs |

**Kafka publishes:** `valuation.updated`, `valuation.approved`

**Integrates with:** Property Registry, Document Service, Auth (`APPRAISER` role), Marketplace (listing price hints)

**Key entities:** `ValuationRequest`, `ApprovedValuation`, `NavSnapshot`

---

#### 12.3.2 Audit Ledger Service (`audit-ledger-service`, :8096)

**Why:** RWA platforms need an immutable audit trail separate from mutable business databases — for regulators, internal forensics, and investor disputes.

| Responsibility | Details |
|----------------|---------|
| Append-only store | No UPDATE/DELETE on audit rows; corrections = new compensating entries |
| Event capture | KYC decisions, document verify/reject, admin overrides, settlement milestones |
| Query APIs | Paginated trail by investor, flat, building, or admin actor |
| Export | Regulatory audit package generation |
| Retention | Configurable retention policy; archive to cold storage (future) |

**Kafka consumes:** `investor.kyc-approved`, `investor.kyc-revoked`, `document.verified`, `trade.settled`, admin action events

**Integrates with:** Compliance, Document, Settlement Saga, Auth (actor attribution)

**Key entities:** `AuditEntry` (immutable)

---

#### 12.3.3 Corporate Actions Service (`corporate-actions-service`, :8097)

**Why:** Dividend distribution, holder snapshots, and future corporate events (splits, rights) are a distinct lifecycle from token deployment and transfers. Extracting this from Issuance reduces Issuance complexity as the platform grows.

| Responsibility | Details |
|----------------|---------|
| Dividend orchestration | Holder snapshot at record date; trigger pro-rata distribution via Issuance/Payment |
| Rent linkage | Consume `rent.collected` → initiate distribution workflow |
| Future actions | Schema for stock splits, rights issues (implementation deferred) |
| Investor visibility | Action history per token/flat |

**Leave-alone:** Token deploy, `operatorTransfer`, on-chain whitelist sync stay in Issuance

**Kafka consumes:** `rent.collected`, `dividend.distributed`, `payout.completed`

**Kafka publishes:** `dividend.distribution-requested` (or REST to Issuance in v1)

**Integrates with:** Token Issuance, Payment, Rental, Notification

**Key entities:** `CorporateAction`, `HolderSnapshot`, `DividendAction`

---

### 12.4 Tier 3 — Scale & integrations (later)

#### 12.4.1 Search Service (`search-service`, :8098)

**Why:** PostgreSQL `LIKE`/filter queries on listings and buildings do not scale to full-text search, faceted filters, or geo queries.

| Responsibility | Details |
|----------------|---------|
| Search index | OpenSearch/Elasticsearch for listings, buildings, public document metadata |
| Index sync | Kafka-driven incremental updates |
| APIs | Full-text search, filters (city, price range, yield, status), geo radius (optional) |

**Kafka consumes:** `listing.created`, `flat.tokenized`, `building.approved`, `valuation.updated`

---

#### 12.4.2 Integration Hub Service (`integration-hub-service`, :8099)

**Why:** Third-party adapters (Onfido, Sumsub, Pinata, future Stripe/MoonPay) are scattered across Compliance, Document, and Payment. A hub centralizes credentials, retries, webhook normalization, and observability.

| Responsibility | Details |
|----------------|---------|
| Outbound adapters | Unified client layer for KYC, IPFS, fiat on-ramp providers |
| Inbound webhooks | Normalize signatures and route to domain services |
| Retry & DLQ | Per-integration retry policy and dead-letter queue |
| Credential rotation | Env/KMS-backed secrets without redeploying domain services |

**Migration path:** Compliance Onfido/Sumsub webhooks first; Document Pinata second; Payment fiat rails last

---

### 12.5 Explicit non-goals (do not spin out)

| Candidate | Decision | Rationale |
|-----------|----------|-----------|
| Limits / policy service | **Reject** | Rules stay in Compliance + Marketplace ([investment-limits.md](rules/investment-limits.md)) |
| Config service | **Reject** | Over-engineering for MVP ([java-architect skill](../.cursor/skills/java-architect/SKILL.md)) |
| Payment escrow split | **Reject** | Escrow lifecycle is cohesive in PaymentService |
| Issuance core split | **Reject** | Deploy/transfer/whitelist stay together; only Corporate Actions extracted |
| Property Registry rename | **Deferred** | Folder stays `token-realty-app/` ([ADR 006](adr/006-monorepo-layout.md)) |

### 12.6 Track backlog (starting 278)

Phase 6 implementation tracks begin at **278**. Suggested batching (25 tracks per merge commit):

| Track range | Focus |
|-------------|-------|
| 278–302 | Reporting Service scaffold + Kafka projections + admin BFF |
| 303–327 | Settlement Saga Service + stuck detection + admin UI |
| 328–352 | Valuation/NAV + Audit Ledger |
| 353–377 | Corporate Actions extraction + Search/Integration Hub (as needed) |
| 378–402 | Phase 6 hardening: Search BFF, settlement outbox, revaluation schedules, document webhooks, outbox ITs |

### 12.7 Tier 4 — Phase 6 hardening (implemented)

Cross-service follow-ups after all seven Phase 6 services scaffolded:

| Area | Deliverable |
|------|-------------|
| Search BFF | Gateway pass-through `GET /v1/bff/search/listings`, `/buildings` |
| Settlement outbox | `settlement.stuck`, `settlement.recovered` events on SLA breach + admin retry |
| Valuation schedules | `RevaluationSchedule` entity + job + admin APIs |
| Integration Hub | Storage webhook relay → Document service ack stub |
| Outbox ITs | Valuation, Corporate Actions, Settlement relay tests |
| Issuance local | `rent-collected-listener-enabled: false` when Corporate Actions owns dividend path |

**Still deferred:** OpenSearch backend, Payment webhooks, KMS credential rotation.

---

## 13. Phase 7 — Event Mesh & Production Integrations

Phases 0–6 delivered 19 backend microservices. Phase 7 **does not add new services** — it completes the Kafka event mesh and production integration paths deferred from Phase 6.

Human-readable guide: [rules/phase-7-services.md](rules/phase-7-services.md).

### 13.1 Tier 1 — Event mesh (implemented)

- [x] Registry consumes `valuation.updated` → syncs flat `Valuation` + token price
- [x] Valuation publishes `valuation.approved` on approve (outbox, same TX as `valuation.updated`)
- [x] Notification consumes `settlement.stuck`, `settlement.recovered`, `valuation.approved`
- [x] Reporting consumes `settlement.stuck` → `StuckSagaRecord` projection
- [x] Audit Ledger consumes `settlement.recovered`, `valuation.approved`
- [x] Kafka integration tests per consumer

### 13.2 Tier 2 — Integrations (implemented)

- [x] Integration Hub payment webhook relay (`POST /v1/integrations/webhooks/payment/{provider}`)
- [x] Payment service webhook endpoint (`POST /v1/payments/webhooks/{provider}`)
- [x] Search index enrichment from Property Registry (building name, city)
- [x] Document storage webhook processing (`StorageWebhookEvent` persistence, digest dedupe)

### 13.3 Tier 3 — Scale (implemented)

- [x] OpenSearch backend for Search service (`SEARCH_BACKEND=opensearch`, compose profile `opensearch`)
- [x] Integration Hub KMS credential rotation (local AES default; `INTEGRATION_KMS_MODE=kms` for cloud KMS stub)
- [x] Corporate Actions stock split (`POST /v1/corporate-actions/stock-splits`, `splitRatio` column)

### 13.4 Track backlog (starting 403)

| Track range | Focus |
|-------------|-------|
| 403–427 | Event mesh — settlement + valuation consumers |
| 428–452 | Integration Hub payment; Search enrichment |
| 453–477 | OpenSearch; KMS; corporate action splits |

---

## 14. Diagram Index

| File | Description |
|------|-------------|
| [`diagrams/01-platform-overview.puml`](diagrams/01-platform-overview.puml) | Current two-service setup |
| [`diagrams/02-target-architecture.puml`](diagrams/02-target-architecture.puml) | Full target microservices architecture |
| [`diagrams/03-token-lifecycle.puml`](diagrams/03-token-lifecycle.puml) | Token lifecycle sequence (8 steps) |
| [`diagrams/04-sell-flow.puml`](diagrams/04-sell-flow.puml) | Primary market buy flow |
| [`diagrams/05-rent-flow.puml`](diagrams/05-rent-flow.puml) | Rent collection → dividend flow |
| [`diagrams/06-kafka-events.puml`](diagrams/06-kafka-events.puml) | Event bus topology |
| [`diagrams/07-build-phases.puml`](diagrams/07-build-phases.puml) | Implementation phase timeline (Phases 0–6) |

**Render PlantUML:** Use [PlantUML online](https://www.plantuml.com/plantuml/uml/), VS Code PlantUML extension, or `plantuml docs/diagrams/*.puml`.

---

## 15. Open Questions & Decisions

| # | Question | Options | Decision |
|---|----------|---------|----------|
| 1 | Token standard | ERC-1400 (partitioned) vs ERC-20 + compliance hook | **ERC-1400** — [ADR 001](adr/001-erc1400-token-standard.md) |
| 2 | Primary payment currency | USDC only vs multi-token (USDC, MATIC, ETH) | **USDC** — [ADR 003](adr/003-usdc-primary-currency.md) |
| 3 | Wallet model | Custodial vs non-custodial vs hybrid | **Hybrid** — [ADR 004](adr/004-hybrid-wallet-model.md) |
| 4 | KYC provider | Sumsub, Onfido, manual, or combination | **Sumsub + Onfido** webhooks/clients; manual dev seed |
| 5 | IPFS pinning | Pinata (hosted) vs self-hosted IPFS node | **Pinata prod; kubo/self-hosted opt-in** |
| 6 | Event schema format | Avro + Schema Registry vs plain JSON | **JSON default; Avro opt-in** — [ADR 002](adr/002-kafka-avro-opt-in.md) |
| 7 | API Gateway tech | Spring Cloud Gateway vs Kong vs nginx | **Servlet proxy (defer SCG)** — [ADR 005](adr/005-servlet-gateway-proxy.md) |
| 8 | Folder rename | Keep `token-realty-app` vs rename to `property-registry-service` | **Deferred** — keep current folder name |
| 9 | Monorepo vs polyrepo | Single repo (current) vs separate repos per service | **Monorepo** — [ADR 006](adr/006-monorepo-layout.md) |
| 10 | Target chain | Polygon mainnet vs Amoy testnet for MVP | Amoy for staging (already configured) |
| 11 | Phase 6 first service | Reporting vs Settlement Saga vs Valuation | **Reporting** (`:8093`) — lowest risk, read-only, immediate admin value |
| 12 | Corporate Actions extraction | Keep in Issuance vs dedicated service | **Dedicated service** — Issuance keeps deploy/transfer only |
| 13 | Search backend | PostgreSQL vs OpenSearch | **OpenSearch** when listing/building count exceeds ~10k or full-text needed |

---

## 16. Cursor Rules & Coding Standards

Agent and IDE conventions live in `.cursor/rules/` (adapted from Titan fintech rules).

| Rule file | Purpose |
|-----------|---------|
| [`.cursor/rules/spring-java-services.mdc`](../.cursor/rules/spring-java-services.mdc) | Layering, transactions, API, security, testing |
| [`.cursor/rules/lombok.mdc`](../.cursor/rules/lombok.mdc) | Lombok on entities, services; records for DTOs |
| [`.cursor/rules/java-dtos.mdc`](../.cursor/rules/java-dtos.mdc) | Typed request/response records; no Map in controllers |
| [`.cursor/rules/kafka-messaging.mdc`](../.cursor/rules/kafka-messaging.mdc) | Topics, envelope, outbox, consumers |
| [`.cursor/rules/rest-client-errors.mdc`](../.cursor/rules/rest-client-errors.mdc) | Inter-service RestClient error mapping |
| [`.cursor/rules/business-exception.mdc`](../.cursor/rules/business-exception.mdc) | ProblemDetail & typed exceptions |
| [`.cursor/rules/pagination.mdc`](../.cursor/rules/pagination.mdc) | List endpoint paging defaults |
| [`.cursor/rules/payment-ledger.mdc`](../.cursor/rules/payment-ledger.mdc) | Escrow, ledger, idempotency (Payment Service) |
| [`.cursor/rules/investment-limits.mdc`](../.cursor/rules/investment-limits.mdc) | KYC gates, min investment, compliance order |

Human-readable guides: [docs/rules/](rules/)

Entry points for agents: [AGENTS.md](../AGENTS.md), root [README.md](../README.md)

### Layout convention

- **Layered:** `token-realty-app`, `token-issuance-service`
- **Layered + kafka/outbox:** `marketplace-service` (template for new services)
- **Target:** light hexagonal (`adapter/in|out`, `application/service`) for Payment, Auth

---

*This document is the living specification for TokenRealty. Update checkboxes and decisions as implementation progresses.*
