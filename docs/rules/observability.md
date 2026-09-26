# Observability — TokenRealty

Shared observability defaults via `tokenrealty-web` (auto-configured on all services that depend on it).

## Features

| Feature | Implementation |
|---------|----------------|
| Trace ID | `X-Trace-Id` header + MDC `traceId` on every inbound request (`TraceIdFilter`) |
| Trace propagation | Outbound `RestClient` copies MDC `traceId` → `X-Trace-Id` (`ServiceRestClientBuilder`) |
| Structured logging | JSON console when profile `json-log` or `prod` (Logstash encoder) |
| Metrics | Micrometer + Prometheus registry (transitive via `tokenrealty-web`) |
| Distributed tracing | Optional OTLP via `application-otel.yml` + `OTEL_ENABLED=true` (Micrometer OTel bridge in `tokenrealty-web`) |
| Actuator | `/actuator/health`, `/info`, `/metrics`, `/prometheus` exposed |

Header and MDC constants: `com.tokenrealty.web.rest.RestHeaders` (`TRACE_ID`, `TRACE_ID_MDC`).

## Dev (default)

Text logs with trace ID:

```text
2026-09-26T16:00:00.000+06:00 INFO  [marketplace-service,abc-123] c.t.m.service.OrderService - ...
```

The second bracket value is the trace ID from MDC.

## JSON logging

```bash
spring.profiles.active=json-log
# or prod profile
```

Logback config: `tokenrealty-web/src/main/resources/logback-spring.xml`.

## OpenTelemetry (local)

Start Jaeger with the compose `otel` profile (or `./scripts/demo-start.sh`):

```bash
docker compose --profile otel up -d
# UI: http://localhost:16686
```

Enable export on any service:

```bash
OTEL_ENABLED=true OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318 \
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=local,otel
```

Shared defaults: `tokenrealty-web/src/main/resources/application-otel.yml`.

## Prometheus scrape

Each service (context-path `/api`):

```text
GET http://localhost:8084/api/actuator/prometheus
```

Gateway:

```text
GET http://localhost:8080/actuator/prometheus
```

Actuator path allowlist: `ActuatorSecurityPaths` in `tokenrealty-security`.

## Trace propagation flow

```text
Browser / Gateway
    │  X-Trace-Id (optional)
    ▼
TraceIdFilter  →  MDC traceId  →  log pattern / JSON field
    │
    ▼
ServiceRestClientBuilder interceptor  →  X-Trace-Id on outbound RestClient
    │
    ▼
Downstream service TraceIdFilter (same or new ID if header missing)
```

Inbound: if the client sends `X-Trace-Id`, it is preserved; otherwise a UUID is generated.

Outbound: only services using `ServiceRestClientBuilder` propagate the current MDC value. Legacy hand-rolled clients may omit the header until migrated — see [rest-client-errors.md](rest-client-errors.md).

## Gateway rate limiting

Per-IP request limits are configured on API Gateway only (`tokenrealty.gateway.rate-limit.*`). See [api-gateway-bff.md](api-gateway-bff.md).

## Related

- [shared-libraries.md](shared-libraries.md) — `tokenrealty-web` observability + REST packages
- [api-gateway-bff.md](api-gateway-bff.md) — BFF, SSE, rate limiting, Docker build
- [e2e-testing.md](e2e-testing.md) — demo-start, full Playwright flows
- [rest-client-errors.md](rest-client-errors.md) — outbound adapter pattern
