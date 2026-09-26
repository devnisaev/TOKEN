# Document Service

IPFS document upload and Property Registry integration for **TokenRealty** data rooms.

Port **8088** · Database **`document_service`**

## API

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/v1/documents/upload` | ADMIN, PROPERTY_MANAGER | Multipart upload → IPFS or MinIO → Registry |
| GET | `/v1/documents/{documentId}` | Any | Document metadata from Registry |

## Run

```bash
psql -U postgres -c "CREATE DATABASE document_service;"
./mvnw spring-boot:run
```

## Storage routing

| Document type | Backend | Registry field |
|---------------|---------|----------------|
| Public data room (`TITLE_DEED`, `VALUATION_REPORT`, …) | IPFS (`IPFS_MODE=simulated` or `pinata`) | `ipfsCid` |
| Private (`KYC_DOCUMENT`, `INSURANCE_POLICY`) | MinIO (`MINIO_MODE=simulated` or `minio`) | `storageUrl` |

MinIO compose (`storage` profile):

```bash
docker compose --profile storage up -d
# Console: http://localhost:9001 (minio / minio123)
# Set MINIO_MODE=minio when Document Service should write to the container
```

Swagger: http://localhost:8088/api/swagger-ui.html
