# Typed DTOs — TokenRealty

Adapted from Titan `java-dtos.mdc`. Cursor rule: [`.cursor/rules/java-dtos.mdc`](../../.cursor/rules/java-dtos.mdc).

---

## What we took from Titan

| Area | TokenRealty |
|------|-------------|
| No `Map`/`Object` in controller signatures | Same |
| Grouped `*Dtos.java` holder classes | `PropertyDtos`, `MarketplaceDtos`, `AuthDtos`, `PaymentDtos` |
| Typed inter-service client responses | `TokenIssuanceClient`, `PropertyRegistryClient` |
| `@Valid` on requests | Same |
| Map only at external/exception boundaries | Hardhat JSON, validation field errors |

## What we skipped

| Titan | TokenRealty |
|-------|-------------|
| `AdminMutationResponse` | Standard typed responses |
| `ApprovalPayload` / maker-checker | Not implemented |
| DB permission resolution in DTO layer | JWT roles + `@PreAuthorize` |
| `application/command/` mandatory | Services take request records directly for now |
| Full hexagonal `adapter/in/web/dto/` everywhere | `dto/` package in layered services |

## Kafka event payloads

| Service | Pattern |
|---------|---------|
| Payment | Typed records in `kafka/events/` — `PaymentConfirmedEvent`, `RentCollectedEvent` |
| Marketplace | `Map<String, Object>` in outbox (migrate to typed records) |

## Known gaps

| Location | Current | Target |
|----------|---------|--------|
| `MarketplaceEventPublisher` | `Map<String, Object>` payload | Typed events like Payment Service |
| `ContractDeployer` | `Map` from Hardhat | OK at boundary — map to typed deploy result |

## File layout by service type

**Layered:** `com.tokenrealty.<service>.dto.*Dtos.java`  
**New services:** same today; migrate to `adapter/in/web/dto/` incrementally.
