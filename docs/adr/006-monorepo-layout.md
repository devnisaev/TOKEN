# ADR 006: Monorepo Layout

**Status:** Accepted (2026-09-26)

## Decision

Retain the **single monorepo** layout: shared libraries (`tokenrealty-*`), 12 microservices, 3 frontends, Hardhat contracts, and unified CI.

## Rationale

- Atomic cross-service changes (Kafka events, OpenAPI codegen, BFF) ship in one PR.
- `./token-realty-app/mvnw test` validates the full backend graph.
- Polyrepo overhead not justified at current team scale.

## Consequences

- Property Registry remains in `token-realty-app/` folder name (rename deferred).
- Docker CI matrix builds all HTTP services from shared Dockerfile.
