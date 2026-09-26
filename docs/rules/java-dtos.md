# Typed DTOs — TokenRealty

Adapted from Titan `java-dtos.mdc`. Cursor rule: [`.cursor/rules/java-dtos.mdc`](../../.cursor/rules/java-dtos.mdc).

---

## What we took from Titan

| Area | TokenRealty |
|------|-------------|
| No `Map`/`Object` in controller signatures | Same |
| Grouped `*Dtos.java` holder classes | `PropertyDtos`, `MarketplaceDtos`, `ComplianceDtos`, `AuthDtos`, `PaymentDtos` |
| Typed inter-service client responses | `ComplianceClient`, `TokenIssuanceClient`, `PropertyRegistryClient` |
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
| Payment | Typed records in `kafka/events/`; port methods take `PaymentConfirmedEvent`, `RentCollectedEvent` |
| Marketplace | Typed records nested in `kafka/port/*Publisher` interfaces (e.g. `OrderMatchedPublisher.OrderMatchedEvent`) |
| Issuance | `TransferCompletedPublisher.TransferCompletedEvent` includes `orderId`, `tradeId`, `paymentId` for settlement |
| Compliance | `KycEventPublisher` port; verify/revoke payloads include `countryCode`, `kycExpiresAt` |

**Consumers:** parse via `kafka/command/*Command.from(KafkaJsonEvent)` — not raw `Map` in listeners. Commands carry only fields the handler needs (e.g. `TransferCompletedCommand` omits wallet addresses).

Outbox adapters map typed events → `OutboxPayload` fields — not raw `Map` in service layer. See [kafka-messaging.md](kafka-messaging.md).

## Known gaps

| Location | Current | Target |
|----------|---------|--------|
| `ContractDeployer` | `Map` from Hardhat | OK at boundary — map to typed deploy result |

## File layout by service type

**Layered:** `com.tokenrealty.<service>.dto.*Dtos.java`  
**New services:** same today; migrate to `adapter/in/web/dto/` incrementally.
