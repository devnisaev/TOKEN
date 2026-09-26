# Observability — TokenRealty

Shared observability defaults via `tokenrealty-web` (auto-configured on all services that depend on it).

## Features

| Feature | Implementation |
|---------|----------------|
| Trace ID | `X-Trace-Id` header + MDC `traceId` on every request (`TraceIdFilter`) |
| Structured logging | JSON console when profile `json-log` or `prod` (Logstash encoder) |
| Metrics | Micrometer + Prometheus registry (transitive via `tokenrealty-web`) |
| Actuator | `/actuator/health`, `/info`, `/metrics`, `/prometheus` exposed |

## Dev (default)

Text logs with trace ID:

```text
2026-09-26T16:00:00.000+06:00 INFO  [marketplace-service,abc-123] c.t.m.service.OrderService - ...
```

## JSON logging

```bash
spring.profiles.active=json-log
# or prod profile
```

## Prometheus scrape

Each service (context-path `/api`):

```text
GET http://localhost:8084/api/actuator/prometheus
```

Gateway:

```text
GET http://localhost:8080/actuator/prometheus
```

## Propagation

Downstream calls should forward `X-Trace-Id` (RestClient interceptors — planned). Browser → Gateway → services: gateway generates or passes through trace ID.
