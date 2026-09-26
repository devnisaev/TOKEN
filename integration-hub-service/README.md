# Integration Hub Service

Central inbound webhook gateway and third-party adapter hub for **TokenRealty**.

Port **8099** · Database **`integration_hub_service`**

## API

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/v1/integrations/webhooks/kyc/{provider}` | Public | Receive KYC webhook; relay to Compliance |
| GET | `/v1/integrations/deliveries` | ADMIN | Paginated delivery audit trail |

Webhook POST accepts the raw JSON body and forwards it to Compliance `POST /v1/compliance/webhooks/kyc/{provider}` with `X-Payload-Digest` and `X-Signature` headers preserved.

## Delivery tracking

Each inbound webhook is stored as an `IntegrationDelivery`:

| Field | Description |
|-------|-------------|
| `integrationType` | `KYC` (v1) |
| `provider` | e.g. `sumsub`, `onfido` |
| `payload` | Raw webhook body |
| `status` | `PENDING`, `DELIVERED`, `FAILED` |
| `attempts` | Forward attempt count |
| `nextRetryAt` | Scheduled retry instant |
| `lastError` | Last downstream failure message |

Failed deliveries are retried by a scheduled worker (max **5** attempts, exponential backoff from **30s** base).

## Run

```bash
psql -U postgres -c "CREATE DATABASE integration_hub_service;"
./token-realty-app/mvnw -f integration-hub-service/pom.xml spring-boot:run
```

Tests:

```bash
./token-realty-app/mvnw -f integration-hub-service/pom.xml test
```

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `services.compliance.url` | `http://localhost:8087/api` | Compliance service base URL |
| `tokenrealty.integration.retry.max-attempts` | `5` | Max forward attempts |
| `tokenrealty.integration.retry.base-backoff-seconds` | `30` | Exponential backoff base |
| `tokenrealty.integration.retry.poll-ms` | `5000` | Retry worker poll interval |

See [docs/PLATFORM-SPEC.md](../docs/PLATFORM-SPEC.md) §10.18 and §12.4.2.
