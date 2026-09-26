# Notification Service

Email notifications driven by Kafka events for **TokenRealty**.

Port **8089** · Database **`notification_service`**

## API

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/v1/notifications/send` | ADMIN | Manual notification trigger |

## Kafka consumers

- KYC approved/revoked
- Trade settled
- Dividend distributed
- Rent collected

Dev mode logs emails instead of sending SMTP.

## Run

```bash
psql -U postgres -c "CREATE DATABASE notification_service;"
./mvnw spring-boot:run
```

Swagger: http://localhost:8089/api/swagger-ui.html
