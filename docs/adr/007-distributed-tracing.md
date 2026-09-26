# ADR 007: Distributed Tracing (OpenTelemetry)

**Status:** Accepted (2026-09-26)

## Decision

Use **OpenTelemetry OTLP export** with optional Jaeger in the compose `otel` profile. Propagate `traceId` via `tokenrealty-web` `TraceIdFilter` into structured logs.

## Rationale

- Micrometer + Prometheus covers metrics; traces close the loop for cross-service buy/rent flows.
- OTLP is vendor-neutral (Jaeger, Grafana Tempo, Datadog agents).
- No mandatory tracing in CI — opt-in via compose profile keeps PR builds fast.

## Consequences

- Gateway and services log JSON with `traceId` when OTEL agent/env is configured.
- Full trace UI requires `docker compose --profile otel up`.
