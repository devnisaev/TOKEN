# TokenRealty Platform — Implementation Spec & TODO

> **Version:** 1.2  
> **Date:** 2025-09-25  
> **Status:** In progress — Payment Service MVP implemented  
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
12. [Diagram Index](#12-diagram-index)
13. [Open Questions & Decisions](#13-open-questions--decisions)
14. [Cursor Rules & Coding Standards](#14-cursor-rules--coding-standards)

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

### Existing services

| Service | Folder | Port | Database | Status |
|---------|--------|------|----------|--------|
| Property Registry | `token-realty-app/` | 8081 | `property_registry` | Implemented (REST, tests, Liquibase defined) |
| Token Issuance | `token-issuance-service/` | 8082 | `token_issuance` | Implemented (REST, tests; blockchain stubbed) |
| Marketplace | `marketplace-service/` | 8084 | `marketplace_service` | **MVP implemented** (listings, buy orders, outbox stub) |
| Auth | `auth-service/` | 8083 | `auth_service` | **MVP implemented** (JWT, refresh, service accounts) |
| Payment | `payment-service/` | 8085 | `payment_service` | **MVP implemented** (escrow, payouts, ledger stub) |

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
- [ ] **Wallet** — not started
- [x] **Marketplace** — MVP done; Payment integration and Kafka relay wired
- [x] **Root README / platform docs** — [README.md](../README.md), [AGENTS.md](../AGENTS.md), Cursor rules
- [ ] **`FULLY_SOLD` status** — enum exists, no service logic sets it

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

#### 4.9 API Gateway / BFF (`api-gateway`, :8080)

- Single entry point for web/mobile clients
- Route to backend services
- Aggregate responses (flat + token price + listing in one call)

#### 4.10 Blockchain Indexer (optional, high value)

- Listen to on-chain events (Transfer, DividendPaid, WhitelistUpdated)
- Sync state to DB; reconcile with Payment Service
- Replace trust in post-tx local balance updates

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
- [ ] Define shared event schema (Avro or JSON Schema in `docs/schemas/`)
- [ ] Create `tokenrealty-events` shared library (Java records + serializers)
- [ ] Add `@KafkaListener` stubs in each service as they are built

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

**Infrastructure (non-HTTP):**

| Component | Port |
|-----------|------|
| PostgreSQL | 5432 |
| Kafka | 9092 |
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

---

## 9. Foundation Fixes (Phase 0)

> **Must complete before new microservices.** Without these, the platform cannot operate end-to-end.

### 9.1 Smart contracts

- [x] Implement `PropertyToken.sol` (ERC-1400 or ERC-20 + compliance hook)
- [x] Implement `ComplianceRegistry.sol` (whitelist add/remove/check)
- [ ] Implement `DividendDistributor.sol` (optional; or dividend logic in PropertyToken)
- [x] Fix file naming: `Propertytoken.sol` → `PropertyToken.sol`, `Complianceregistry.sol` → `ComplianceRegistry.sol`
- [x] Create `hardhat/scripts/deployFlat.js` (called by `ContractDeployer`)
- [x] Run `npm run compile` and verify artifacts generated
- [x] Test deploy on local Hardhat node (`hardhat/scripts/deploy.js` → `deployments/localhost.json`)
- [x] Copy contract addresses to profiles (`export-env.sh`, `local` Spring profiles — see [hardhat-demo.md](hardhat-demo.md))

### 9.2 Inter-service authentication

- [x] Add service account credentials to Auth Service (`DevDataInitializer`)
- [x] Configure `PropertyRegistryClient` / `TokenIssuanceClient` RestClient with Bearer service token
- [ ] Test Issuance → Registry `PATCH /v1/flats/{id}/token-info` callback end-to-end
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
- [ ] Create root `README.md` with platform overview and startup order
- [ ] Sync API tables in service READMEs with actual controllers
- [ ] Update Property Registry "Next Steps" (mark Issuance integration partial)

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
- [ ] Rate limiting (optional)
- [x] Health check aggregation endpoint (`GET /actuator/platform-health`)

### 10.3 Kafka infrastructure (Phase 1)

- [ ] Root `docker-compose.yml` with Kafka, Zookeeper, Schema Registry
- [ ] Shared event library (`tokenrealty-events/`)
- [ ] Define all event schemas (see Section 6.2)
- [x] Add Kafka producers to Property Registry (BuildingApproved, FlatTokenized)
- [ ] Add Kafka consumers in Notification Service (stub)

### 10.4 Marketplace Service (Phase 2)

- [x] Scaffold project (`marketplace-service/`)
- [x] Entities: Listing, MarketOrder, Trade, OutboxEvent
- [x] POST `/v1/listings` — create listing for tokenized flat
- [x] GET `/v1/listings` — search/filter listings
- [x] POST `/v1/orders` — place buy order (primary market)
- [x] Simple order match on buy (reserve tokens, status MATCHED)
- [x] KYC check via Compliance Service `GET /v1/compliance/check/{wallet}` (`ComplianceClient`)
- [x] Outbox publisher for `listing.created`, `order.matched`, `trade.settled`
- [x] Admin PATCH `/v1/orders/{id}/settle` (fallback; automated flow via Kafka when enabled)
- [x] Unit + context tests (`./mvnw test`)
- [x] Secondary market sell orders (`POST /v1/listings/secondary`, `POST /v1/orders/sell`)
- [x] Kafka relay (outbox → broker via `OutboxRelayWorker`)
- [x] Consumer: auto-create listing on `flat.tokenized`
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
- [ ] Webhook endpoint for Sumsub/Onfido (optional)
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
- [ ] Optional S3/MinIO for private KYC/legal docs (`storageUrl`)

#### Phase 4b-3 — Deferred

- [ ] `energyEfficiencyRating`, `zoningCode` on `Building`
- [ ] `lastRenovationYear` on `Building`
- [ ] Non-equity token structures (`PART_DEBT_INSTRUMENT`, `PROFIT_SHARING_AGREEMENT`) in Issuance/Marketplace

### 10.8 Document Service (Phase 4)

- [x] Scaffold project (`document-service/`)
- [x] POST `/v1/documents/upload` — multipart upload → IPFS
- [x] GET `/v1/documents/{documentId}` — retrieve metadata from Registry
- [x] IPFS client (simulated dev mode + optional Pinata API)
- [ ] Optional S3/MinIO for private documents
- [x] Callback to Property Registry with CID
- [x] Publish `DocumentUploaded` event (outbox)
- [x] Unit + context tests

### 10.9 Notification Service (Phase 5)

- [x] Scaffold project (`notification-service/`)
- [x] Email provider integration (log mode dev + optional SMTP)
- [x] Kafka consumers for key notification events (KYC, trade settled, dividend, rent)
- [x] Simple email templates per event type
- [x] POST `/v1/notifications/send` — manual trigger (admin)
- [ ] Notification preferences per user

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
- [x] Seed data script for demo buildings + flats + test users

### 11.3 Observability

- [ ] Structured logging (JSON) with `traceId` across services
- [ ] Micrometer + Prometheus metrics
- [ ] Distributed tracing (OpenTelemetry / Zipkin)
- [ ] Health checks: `/actuator/health` on all services

### 11.4 CI/CD

- [ ] GitHub Actions: build + test per service
- [ ] Contract compile + test in CI
- [ ] Docker image build per service
- [ ] Integration test suite with Testcontainers (PostgreSQL, Kafka)

### 11.5 Frontend (out of scope for backend spec, noted for completeness)

- [ ] Investor portal (browse listings, buy tokens, view portfolio, dividends)
- [ ] Admin dashboard (manage properties, KYC review, token issuance)
- [ ] Tenant portal (pay rent, view lease)

---

## 12. Diagram Index

| File | Description |
|------|-------------|
| [`diagrams/01-platform-overview.puml`](diagrams/01-platform-overview.puml) | Current two-service setup |
| [`diagrams/02-target-architecture.puml`](diagrams/02-target-architecture.puml) | Full target microservices architecture |
| [`diagrams/03-token-lifecycle.puml`](diagrams/03-token-lifecycle.puml) | Token lifecycle sequence (8 steps) |
| [`diagrams/04-sell-flow.puml`](diagrams/04-sell-flow.puml) | Primary market buy flow |
| [`diagrams/05-rent-flow.puml`](diagrams/05-rent-flow.puml) | Rent collection → dividend flow |
| [`diagrams/06-kafka-events.puml`](diagrams/06-kafka-events.puml) | Event bus topology |
| [`diagrams/07-build-phases.puml`](diagrams/07-build-phases.puml) | Implementation phase timeline |

**Render PlantUML:** Use [PlantUML online](https://www.plantuml.com/plantuml/uml/), VS Code PlantUML extension, or `plantuml docs/diagrams/*.puml`.

---

## 13. Open Questions & Decisions

| # | Question | Options | Decision |
|---|----------|---------|----------|
| 1 | Token standard | ERC-1400 (partitioned) vs ERC-20 + compliance hook | TBD — ERC-1400 described in README |
| 2 | Primary payment currency | USDC only vs multi-token (USDC, MATIC, ETH) | TBD — recommend USDC for stability |
| 3 | Wallet model | Custodial vs non-custodial vs hybrid | TBD — hybrid recommended |
| 4 | KYC provider | Sumsub, Onfido, manual, or combination | TBD |
| 5 | IPFS pinning | Pinata (hosted) vs self-hosted IPFS node | TBD |
| 6 | Event schema format | Avro + Schema Registry vs plain JSON | TBD |
| 7 | API Gateway tech | Spring Cloud Gateway vs Kong vs nginx | TBD |
| 8 | Folder rename | Keep `token-realty-app` vs rename to `property-registry-service` | TBD |
| 9 | Monorepo vs polyrepo | Single repo (current) vs separate repos per service | TBD — monorepo fine for now |
| 10 | Target chain | Polygon mainnet vs Amoy testnet for MVP | Amoy for staging (already configured) |

---

## 14. Cursor Rules & Coding Standards

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
