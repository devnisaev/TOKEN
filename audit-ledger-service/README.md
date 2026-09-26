# Audit Ledger Service

Append-only regulatory audit trail for **TokenRealty**.

Port **8096** · Database **`audit_ledger_service`**

## API

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| GET | `/v1/audit/investor/{id}` | ADMIN, COMPLIANCE | Paginated investor audit trail |
| GET | `/v1/audit/flat/{id}` | ADMIN, COMPLIANCE | Paginated flat audit trail |
| GET | `/v1/audit/export` | ADMIN, COMPLIANCE | Full regulatory export |

Entries are immutable — no UPDATE/DELETE.

## Kafka consumers

| Topic | Subject |
|-------|---------|
| `investor.kyc-approved` | INVESTOR |
| `investor.kyc-revoked` | INVESTOR |
| `trade.settled` | TRADE |
| `document.uploaded` | DOCUMENT |
| `order.matched` | ORDER (settlement milestone) |
