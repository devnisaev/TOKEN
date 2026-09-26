---
name: java-architect
description: Design TokenRealty Java/Spring service slices before coding — hexagonal boundaries, commands, Kafka ingest, and leave-alones. Use when the user asks what to improve, how to structure a service, next steps, or wants options before implementation.
---

# Java architect (TokenRealty)

Read `docs/rules/README.md` and the service slice in `hexagonal-boundaries.md` before proposing changes. Do not invent a parallel architecture.

## Workflow

1. Name the service and read its existing slice + matching `.cursor/rules/*.mdc`.
2. Identify HTTP / Kafka / outbound REST / Clock / exception-map gaps.
3. Propose **2–3 options** with leave-alones (what not to split).
4. Pick the smallest option that matches core services (`property-registry`, `token-issuance`, `marketplace`, `payment`, `rental`, `compliance`).
5. Write a numbered 1–6 plan. **Do not edit code** until the user says start.

## Default 1–6 (application slice)

1. HTTP: `DTO.toCommand()` → one service method for 4+ field mutations. GETs with 1–3 args stay positional.
2. Views in `application/dto/*Views` (or existing `*View`) — application never imports `adapter.in.web.dto`.
3. Kafka: `*EventCommand.from(event)` + `*KafkaEventTypes`; claim on the consumer unless the slice says otherwise.
4. `Clock` on `*Config` (not `*KafkaConfig`) when persist/claim/dispatch depends on now — skip ops-only services with no write-path time.
5. `GlobalExceptionHandler` status map + `raiseException` (no `fail()` factory).
6. Docs: slice in `hexagonal-boundaries.md`, README index, `application-commands.mdc`, kafka/REST docs if touched.

## Leave-alones

- Do not split a cohesive engine for layout (`PaymentEngine`, `MarketplaceService`, `TokenIssuanceEngine`).
- Do not add `*KafkaIngestSupport` when listeners have different `from()` payloads.
- Ops readiness maps and unpaged peeks stay as documented.
- Invariants: `BUSINESS_RULES.md`, `event-zero-loss.mdc`, `pci-cde.mdc`.

---

## TokenRealty doc map (use these paths)

This repo uses **layered** services today with **light hexagonal** for new Tier 1+ work — not full Titan-style slices yet.

| Referenced in workflow | TokenRealty equivalent |
|------------------------|-------------------------|
| `docs/rules/README.md` | [docs/README.md](../../../docs/README.md) + [docs/rules/spring-java-services.md](../../../docs/rules/spring-java-services.md) |
| `hexagonal-boundaries.md` | [docs/rules/spring-java-services.md](../../../docs/rules/spring-java-services.md) § layout; [.cursor/rules/spring-java-services.mdc](../../rules/spring-java-services.mdc) |
| `application-commands.mdc` | [.cursor/rules/java-dtos.mdc](../../rules/java-dtos.mdc) — records for DTOs/commands |
| `event-zero-loss.mdc` | [.cursor/rules/kafka-messaging.mdc](../../rules/kafka-messaging.mdc) + [business-rules.mdc](../../rules/business-rules.mdc) (outbox in same TX) |
| `pci-cde.mdc` | **N/A** — use [business-rules.mdc](../../rules/business-rules.mdc) (no private keys / KYC bodies in events) |

## Services (ports & packages)

| Service | Port | Package | Layout |
|---------|------|---------|--------|
| Property Registry | 8081 | `com.tokenrealty.registry` | Layered + kafka out |
| Token Issuance | 8082 | `com.tokenrealty.issuance` | Layered + kafka + blockchain |
| Auth | 8083 | `com.tokenrealty.auth` | Layered |
| Marketplace | 8084 | `com.tokenrealty.marketplace` | Layered + kafka in/out |
| Payment | 8085 | `com.tokenrealty.payment` | Layered + escrow + kafka |
| Rental | 8086 | `com.tokenrealty.rental` | Layered + Payment client |
| Compliance | 8087 | `com.tokenrealty.compliance` | Layered + KYC outbox |
| Document | 8088 | `com.tokenrealty.document` | Layered + IPFS + outbox |
| Notification | 8089 | `com.tokenrealty.notification` | Kafka consumer only |
| API Gateway | 8080 | `com.tokenrealty.gateway` | Proxy, no DB |

Copy new services from `marketplace-service/` or `compliance-service/`.

## Layered layout (current standard)

```text
controller → service → repository → entity
                ↓
            client/*Client                 ← inter-service RestClient
            kafka/command/*Command.java    ← from(KafkaJsonEvent)
            kafka/in/*Listener.java
            kafka/port/*Publisher.java
            kafka/outbox/                  ← when publishing events
```

Shared libs: `tokenrealty-web`, `tokenrealty-jpa`, `tokenrealty-kafka`, `tokenrealty-outbox`, `tokenrealty-security`, `tokenrealty-events` — see [shared-libraries.md](../../../docs/rules/shared-libraries.md).

## TokenRealty-specific leave-alones

- Do not refactor Property Registry / Issuance to full hexagonal unless explicitly requested.
- Do not extract `config-service` or Redis limits engine — compliance API + listing rules suffice for MVP.
- `@Transactional` on service writes; no HTTP/blockchain inside long transactions ([BUSINESS_RULES.md](../../../docs/BUSINESS_RULES.md)).
- Payment escrow lifecycle stays in `PaymentService` — do not split for layout.

## Related skills

- [kafka-event-architect](../kafka-event-architect/SKILL.md) — new topics/listeners
- [rwa-legal-compliance](../rwa-legal-compliance/SKILL.md) — Compliance, Registry, Document
- [web3j-blockchain-integration](../web3j-blockchain-integration/SKILL.md) — Issuance + Payment chain
- [solidity-smart-contract](../solidity-smart-contract/SKILL.md) — Hardhat contracts
