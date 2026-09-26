# ADR 005: Servlet Reverse Proxy Gateway (Defer Spring Cloud Gateway)

**Status:** Accepted (2026-09-26)

## Decision

Keep the **Spring MVC servlet reverse proxy** in `api-gateway` with JWT validation, CORS, rate limiting, and BFF/GraphQL layers. Defer migration to **Spring Cloud Gateway** until Boot 4 reactive gateway compatibility is validated in CI.

## Rationale

- Current gateway is stable with BFF aggregation, SSE order stream, and GraphQL.
- Reactive migration is high churn with limited immediate product value.
- Redis-backed rate limiting already works in compose.

## Consequences

- Route config remains in `GatewayRouteProperties` YAML.
- Future gateway migration tracked as a separate epic, not blocking MVP.
