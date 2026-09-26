# Spring Java Services — TokenRealty

Adapted from Titan `spring-java-services.mdc` for the TokenRealty platform.  
Cursor rule: [`.cursor/rules/spring-java-services.mdc`](../../.cursor/rules/spring-java-services.mdc)

---

## Related rules

| Rule | Scope |
|------|-------|
| [spring-java-services.mdc](../../.cursor/rules/spring-java-services.mdc) | Architecture, layering, transactions, API |
| [lombok.mdc](../../.cursor/rules/lombok.mdc) | Lombok on entities and services |
| [java-dtos.mdc](../../.cursor/rules/java-dtos.mdc) | Typed request/response records |
| [kafka-messaging.mdc](../../.cursor/rules/kafka-messaging.mdc) | Topics, outbox, consumers |
| [rest-client-errors.mdc](../../.cursor/rules/rest-client-errors.mdc) | Inter-service HTTP errors |
| [business-exception.mdc](../../.cursor/rules/business-exception.mdc) | ProblemDetail mapping |
| [pagination.mdc](../../.cursor/rules/pagination.mdc) | List endpoint defaults |
| [payment-ledger.mdc](../../.cursor/rules/payment-ledger.mdc) | Escrow, ledger, payment idempotency (Payment Service) |
| [investment-limits.mdc](../../.cursor/rules/investment-limits.mdc) | KYC gates, min investment rules |
| [business-rules.mdc](../../.cursor/rules/business-rules.mdc) | Tier-1 invariants (ledger, outbox, compliance order) |
| [shared-libraries.md](shared-libraries.md) | Cross-service Maven modules (web, jpa, kafka, outbox, security) |
| [wallet-service.md](wallet-service.md) | Custodial wallets, encryption, aggregate balance |
| [blockchain-indexer.md](blockchain-indexer.md) | On-chain event poll, balance reconciliation |
| [investor-portal.md](investor-portal.md) | React investor UI (Vite, TanStack Query, wagmi) |
| [admin-dashboard.md](admin-dashboard.md) | React admin UI (buildings, KYC review) |
| [api-gateway-bff.md](api-gateway-bff.md) | Gateway BFF aggregate endpoints |
| [phase-6-services.md](phase-6-services.md) | Phase 6 ports 8093–8099, tiers, Kafka matrix |

---

## What we took from Titan

| Area | TokenRealty adaptation |
|------|------------------------|
| Hexagonal architecture | **Light** — layered for legacy; hexagonal for new services |
| Transaction boundaries | Same — no external I/O inside TX |
| Outbox + Kafka | Same pattern — see kafka-messaging |
| Idempotency | Planned for Payment/transfers |
| ProblemDetail errors | Same |
| MapStruct + records | Same |
| `@Version` optimistic locking | Same — `BaseEntity` in `tokenrealty-jpa` |
| Forward-only migrations | Liquibase (not Flyway) |
| Controller → service only | Same |

## What we simplified or skipped

| Titan | TokenRealty |
|-------|-------------|
| Full hexagonal everywhere | Layered OK for registry/issuance |
| `cardsystem-common` Money type | `BigDecimal` + scale 2 for USD |
| Maker-checker approval tables | Simple admin approval later |
| PCI / PAN rules | No private keys in logs/payloads |
| ISO8583, HSM, card ledger engine | N/A — see [payment-ledger.md](payment-ledger.md) for escrow |
| Redis limits / velocity engine | N/A — see [investment-limits.md](investment-limits.md) |
| Multi-tenant | Not yet |

---

## Service layout reference

### Layered (Property Registry, Token Issuance)

```
controller → service → repository → entity
                ↓
            client/ (RestClient)
            kafka/port/*Publisher          ← Registry: FlatTokenizedPublisher
            kafka/outbox/                  ← Registry: outbox + relay (when publishing)
            kafka/in/                      ← Issuance: consumers
```

### New services (Marketplace — template for Auth, Payment, Rental)

```
controller → service → repository → entity (extends tokenrealty-jpa BaseEntity)
                ↓
            client/*Client                 ← inter-service RestClient wrappers
            kafka/command/*Command.java    ← from(KafkaJsonEvent)
            kafka/in/*Listener.java        ← @KafkaListener + KafkaEventConsumer (tokenrealty-kafka)
            kafka/port/*Publisher.java     ← outbox port (typed event record)
            kafka/outbox/Outbox{Event}Publisher
            kafka/outbox/OutboxWriter        ← extends tokenrealty-outbox
            kafka/outbox/OutboxRelayWorker   ← thin wrapper → OutboxRelay.relay(...)
            kafka/outbox/OutboxEvent         ← implements OutboxRelayTarget
```

Shared libs replace per-service copies: exceptions/handler + outbound REST (`tokenrealty-web`), auditing (`tokenrealty-jpa`), idempotency (`tokenrealty-kafka`), RestClient builder (`ServiceRestClientBuilder` in `tokenrealty-security`). See [shared-libraries.md](shared-libraries.md) and [rest-client-errors.md](rest-client-errors.md).

Future extraction path:

```
adapter/in/web → application/service → adapter/out/{persistence,client,kafka}
```

---

## Implemented services

| Service | Port | Package | Layout |
|---------|------|---------|--------|
| API Gateway | 8080 | `com.tokenrealty.gateway` | Reverse proxy + JWT (no DB) |
| Property Registry | 8081 | `com.tokenrealty.registry` | Layered + kafka out |
| Token Issuance | 8082 | `com.tokenrealty.issuance` | Layered + kafka in/out |
| Auth | 8083 | `com.tokenrealty.auth` | Layered + JWT issuer |
| Marketplace | 8084 | `com.tokenrealty.marketplace` | Layered + kafka in/out |
| Payment | 8085 | `com.tokenrealty.payment` | Layered + escrow + kafka in/out |
| Notification | 8089 | `com.tokenrealty.notification` | Kafka consumer + email (log/SMTP) |
| Rental | 8086 | `com.tokenrealty.rental` | Layered + Payment client |
| Compliance | 8087 | `com.tokenrealty.compliance` | Layered + KYC outbox |
| Document | 8088 | `com.tokenrealty.document` | Layered + IPFS upload + Registry callback + outbox |
| Wallet | 8090 | `com.tokenrealty.wallet` | Layered + Web3j sign + Payment/Issuance clients |
| Blockchain Indexer | 8091 | `com.tokenrealty.indexer` | Layered + Web3j poll + scheduled reconciliation |

### API Gateway (edge)

Servlet reverse proxy on `:8080` — Spring Cloud Gateway deferred until Boot 4 compatibility.

```
config/GatewayRouteProperties.java   ← path prefix → downstream URL
config/SecurityConfig.java           ← JWT; public auth register/login/refresh
proxy/GatewayProxyController.java    ← forwards /api/** to services
```

Routes in `application.yml` under `tokenrealty.gateway.routes`. Downstream services keep `/api` context path; gateway forwards full path unchanged.

**Route order matters** — more specific prefixes first. Example: `/api/v1/documents/upload` → Document Service (:8088) before `/api/v1/documents` → Property Registry (:8081).

Clients should call `http://localhost:8080/api/v1/...` instead of individual service ports.

**BFF aggregates** (local handlers, not proxied) — see [api-gateway-bff.md](api-gateway-bff.md):

```
bff/BffController.java          ← GET /v1/bff/flats/{id}, /v1/bff/listings/{id}
bff/BffFlatService.java         ← Registry + Issuance + Marketplace compose
client/PropertyRegistryClient.java, MarketplaceClient.java, TokenIssuanceClient.java
config/ServiceClientConfig.java ← service account api-gateway / gateway-secret
```

CORS: `http://localhost:5173` for Investor Portal dev server.

### Property Registry — document.uploaded consumer

```text
kafka/in/DocumentUploadedListener.java   ← ack CID + idempotent reconcile
service/DocumentService.acknowledgeUpload()
```

Compliance consumes the same event for review queue; verify via `PATCH /v1/compliance/document-reviews/{documentId}/verify` → Registry `PATCH /v1/documents/{id}/verify`.

### Document Service (data room upload)

Handles IPFS pinning and Registry registration — Registry stores metadata only (`PropertyDocument.ipfsCid`).

```
controller/DocumentController.java     ← multipart POST /v1/documents/upload
service/DocumentUploadService.java     ← pin → Registry callback → outbox event
storage/IpfsStorageService.java        ← simulated CID (dev) or Pinata API
client/PropertyRegistryClient.java     ← POST building/flat documents
kafka/outbox/                          ← document.uploaded
```

Service account: `document` / `document-secret` (ADMIN). Roles for upload: `ADMIN`, `PROPERTY_MANAGER`.

### Wallet Service (custodial + linked wallets)

See [wallet-service.md](wallet-service.md).

```
controller/WalletController.java
service/WalletService.java, CustodialSignService.java
crypto/WalletEncryptionService.java     ← AES-GCM; never log keys
client/PaymentClient.java, IssuanceClient.java
```

### Blockchain Indexer (on-chain sync)

See [blockchain-indexer.md](blockchain-indexer.md).

```
service/BlockchainLogIndexer.java       ← @Scheduled eth_getLogs poll
service/BalanceReconciliationService.java
blockchain/OnChainBalanceReader.java    ← balanceOf eth_call
client/IssuanceClient.java              ← contract + holder lists
```

Token Issuance support endpoint: `GET /v1/investors/{investorId}/holdings`.

---

## Checklist — new microservice

Phase 6 services (ports 8093–8099, **implemented**): see [phase-6-services.md](phase-6-services.md) and [PLATFORM-SPEC.md §12](../PLATFORM-SPEC.md#12-phase-6--planned-services).

- [ ] Spring Boot 4 / Java 21 Maven module
- [ ] Port + DB from [PLATFORM-SPEC.md](../PLATFORM-SPEC.md) §7
- [ ] `SecurityConfig` + `tokenrealty-security` JWT filter
- [ ] `tokenrealty-web` + `tokenrealty-jpa` dependencies (exceptions/handler + BaseEntity auto-config)
- [ ] Outbound `*Client` classes extend `DownstreamRestClientSupport` (uses `RestClientOperations` — no raw `restClient.get/post` in adapters); one `@Bean` RestClient per base URL via `ServiceRestClientBuilder` with explicit timeout
- [ ] `tokenrealty-kafka` if consuming events; `tokenrealty-outbox` if publishing
- [ ] `application.yml` + `application-test.yml` (H2)
- [ ] springdoc OpenAPI
- [ ] README with API table
- [ ] `./mvnw test` passing
- [ ] Link in [docs/README.md](../README.md)
- [ ] Kafka topics in [EVENTS.md](../EVENTS.md) if applicable

---

## Testcontainers integration tests

PostgreSQL integration tests use `@Tag("testcontainers")` and are **excluded** from default `./mvnw test` (Surefire `excludedGroups`). Run explicitly in CI or locally with Docker:

| Test | Service | CI job |
|------|---------|--------|
| `BuyFlowContainersIntegrationTest` | marketplace-service | `marketplace-testcontainers` |
| `PaymentContainersIntegrationTest` | payment-service | `payment-testcontainers` |

Pattern:

```java
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Tag("testcontainers")
class MyContainersIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
    }
}
```

Run one test: `./mvnw test -Dtest=BuyFlowContainersIntegrationTest` (requires Docker).

---

## Notification preferences (JPA)

Notification Service persists per-user settings in `notification_preferences` (`NotificationPreference` entity):

| Method | Path |
|--------|------|
| `GET` | `/v1/notifications/preferences/{userId}` |
| `PATCH` | `/v1/notifications/preferences/{userId}` |

Fields: `emailEnabled`, `tradeAlerts`, `dividendAlerts`, `rentReminders`. Missing rows return defaults (all `true`); `update` upserts by `userId`.

Kafka delivery respects preferences via `NotificationPreferenceGate` — resolves `userId`/`investorId`/`buyerId` from event payload; skips email when the matching alert category is disabled. Admin `POST /v1/notifications/send` bypasses the gate.

Tests: `NotificationPreferenceServiceTest`, `NotificationPreferenceGateTest`, `NotificationLogServiceTest`.

---

## WireMock downstream client tests

Lightweight integration tests for `RestClient` wrappers without a full Spring context:

| Test | Service | Verifies |
|------|---------|----------|
| `PropertyRegistryClientIntegrationTest` | token-issuance-service | `PATCH /v1/flats/{id}/token-info` callback to Registry |

Pattern: start `WireMockServer` on a fixed port, build `RestClient` with `JdkClientHttpRequestFactory` (PATCH support), stub `/api/v1/flats/.+/token-info` when base URL includes `/api`.

Run: `./mvnw test -Dtest=PropertyRegistryClientIntegrationTest` in `token-issuance-service/`. CI job: `issuance-integration-test`.

Kafka ingest integration tests (Spring context + `KafkaEventConsumer`, no Testcontainers Kafka):

| Test | Service | Verifies |
|------|---------|----------|
| `NotificationKafkaIntegrationTest` | notification-service | `trade.settled`, `rent.due`, `rent.collected`, `building.approved`, `lease.expired`, `kyc-revoked` + preference gates + dedupe |
| `DocumentUploadedKafkaIntegrationTest` | property-registry | `document.uploaded` CID/storageUrl backfill + dedupe |
| `DocumentUploadedKafkaIntegrationTest` | compliance-service | Review queue ingest + dedupe |
| `KycApprovedKafkaIntegrationTest` | token-issuance-service | `kyc-approved` → on-chain whitelist + dedupe |
| `KycRevokedKafkaIntegrationTest` | token-issuance-service | `kyc-revoked` → on-chain revoke + dedupe |
| `FlatTokenizedKafkaIntegrationTest` | marketplace-service | `flat.tokenized` → primary listing + dedupe |
| `RentCollectedKafkaIntegrationTest` | token-issuance-service | `rent.collected` → pro-rata dividend + dedupe |
| `OrderMatchedKafkaIntegrationTest` | payment-service | `order.matched` escrow reconciliation + dedupe |
| `DividendDistributedKafkaIntegrationTest` | payment-service | `dividend.distributed` → holder payouts + dedupe |
| `PropertyRegistryIntegrationTest` | property-registry | 17-step building → tokenize regression (H2) |

CI jobs: `kafka-integration-tests`, `registry-integration-test`, `issuance-integration-test`, `marketplace-testcontainers`, `payment-testcontainers`.

---

## Key conventions (quick reference)

```java
// Transaction on public write method
@Transactional
public ListingResponse create(CreateListingRequest request) { ... }

// RestClient bean — auth, timeout, X-Trace-Id (shared builder)
@Bean("tokenIssuanceRestClient")
RestClient tokenIssuanceRestClient(
        @Value("${services.token-issuance.url}") String baseUrl,
        ObjectProvider<ServiceTokenProvider> serviceTokenProvider) {
    return ServiceRestClientBuilder.build(
            baseUrl, Duration.ofSeconds(10), serviceTokenProvider);
}

// Domain client — extend shared base, no try/catch
@Component
public class TokenIssuanceClient extends DownstreamRestClientSupport {
    public TokenIssuanceClient(@Qualifier("tokenIssuanceRestClient") RestClient restClient) {
        super(restClient);
    }
    // get/post/postVoid/patchVoid + DownstreamServices.* for error labels
}

// Exceptions — import from tokenrealty-web, do not copy
throw new ValidationException("Listing is not active");

// Controller
@PostMapping
@PreAuthorize("hasRole('INVESTOR')")
public OrderResponse place(@Valid @RequestBody PlaceOrderRequest request) {
    return orderService.placeBuyOrder(request);
}
```
