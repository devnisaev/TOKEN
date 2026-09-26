# Document Service

IPFS document upload and Property Registry integration for **TokenRealty** data rooms.

Port **8088** · Database **`document_service`**

## API

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/v1/documents/upload` | ADMIN, PROPERTY_MANAGER | Multipart upload → IPFS → Registry CID |
| GET | `/v1/documents/{documentId}` | Any | Document metadata from Registry |

## Run

```bash
psql -U postgres -c "CREATE DATABASE document_service;"
./mvnw spring-boot:run
```

Swagger: http://localhost:8088/api/swagger-ui.html
