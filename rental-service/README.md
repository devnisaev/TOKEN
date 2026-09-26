# Rental Service

Lease management and rent collection for **TokenRealty** tokenized properties.

Port **8086** · Database **`rental_service`**

## API

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/v1/leases` | ADMIN, PROPERTY_MANAGER | Create lease |
| GET | `/v1/leases/{id}` | Any | Lease details |
| GET | `/v1/leases?tenantId=` | Any | List leases for tenant |
| POST | `/v1/rent-payments` | ADMIN, PROPERTY_MANAGER, TENANT | Record rent payment |
| GET | `/v1/rent-payments?leaseId=` | Any | Payment history for lease |
| GET | `/v1/rent-payments/summary` | ADMIN, PROPERTY_MANAGER | Flat rent summary by period |
| GET | `/v1/occupancy/flats/{flatId}` | Any | Occupancy status |

Rent payments trigger Payment Service payouts and Kafka `rent.collected` → dividend pipeline.

## Run

```bash
psql -U postgres -c "CREATE DATABASE rental_service;"
./mvnw spring-boot:run
```

Swagger: http://localhost:8086/api/swagger-ui.html
