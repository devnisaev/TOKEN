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
| `@Version` optimistic locking | Same — `BaseEntity` |
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
            kafka/  (events)
```

### New services (Marketplace — template for Auth, Payment)

```
controller → service → repository → entity
                ↓
            client/TokenIssuanceClient
            kafka/MarketplaceEventPublisher
            kafka/outbox/OutboxEvent
```

Future extraction path:

```
adapter/in/web → application/service → adapter/out/{persistence,client,kafka}
```

---

## Implemented services

| Service | Port | Package | Layout |
|---------|------|---------|--------|
| Property Registry | 8081 | `com.tokenrealty.registry` | Layered |
| Token Issuance | 8082 | `com.tokenrealty.issuance` | Layered |
| Marketplace | 8084 | `com.tokenrealty.marketplace` | Layered + kafka/outbox |
| Auth | 8083 | `com.tokenrealty.auth` | Layered + JWT filter |

---

## Checklist — new microservice

- [ ] Spring Boot 4 / Java 21 Maven module
- [ ] Port + DB from [PLATFORM-SPEC.md](../PLATFORM-SPEC.md) §7
- [ ] `SecurityConfig` + `SecurityUserConfig` split
- [ ] `GlobalExceptionHandler` with ProblemDetail
- [ ] `application.yml` + `application-test.yml` (H2)
- [ ] springdoc OpenAPI
- [ ] README with API table
- [ ] `./mvnw test` passing
- [ ] Link in [docs/README.md](../README.md)
- [ ] Kafka topics in [EVENTS.md](../EVENTS.md) if applicable

---

## Key conventions (quick reference)

```java
// Transaction on public write method
@Transactional
public ListingResponse create(CreateListingRequest request) { ... }

// RestClient with qualifier
@Bean("tokenIssuanceRestClient")
RestClient tokenIssuanceRestClient(@Value("${services.token-issuance.url}") String baseUrl)

// Controller
@PostMapping
@PreAuthorize("hasRole('INVESTOR')")
public OrderResponse place(@Valid @RequestBody PlaceOrderRequest request) {
    return orderService.placeBuyOrder(request);
}
```
