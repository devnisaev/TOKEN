# Compliance Service

KYC whitelist management for **TokenRealty** investors.

Port **8087** · Database **`compliance_service`**

## API

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| GET | `/v1/compliance` | ADMIN, COMPLIANCE | List records |
| GET | `/v1/compliance/{id}` | ADMIN, COMPLIANCE | Get record |
| GET | `/v1/compliance/check/{wallet}` | Any | Wallet whitelist check |
| POST | `/v1/compliance` | ADMIN, COMPLIANCE | Register investor |
| PATCH | `/v1/compliance/{id}/verify` | ADMIN, COMPLIANCE | Approve KYC |
| PATCH | `/v1/compliance/{id}/revoke` | ADMIN, COMPLIANCE | Revoke KYC |

KYC approval publishes `kyc-approved` via outbox → Issuance on-chain whitelist sync.

## Run

```bash
psql -U postgres -c "CREATE DATABASE compliance_service;"
./mvnw spring-boot:run
```

Swagger: http://localhost:8087/api/swagger-ui.html
