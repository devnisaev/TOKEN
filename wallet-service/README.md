# Wallet Service

Custodial wallets and aggregate balance views for **TokenRealty** investors.

Port **8090** · Database **`wallet_service`**

## API

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/v1/wallets/link` | INVESTOR, ADMIN | Link MetaMask wallet |
| GET | `/v1/wallets/{investorId}/balance` | Any | USDC + token holdings aggregate |
| POST | `/v1/wallets/{investorId}/sign` | ADMIN | Sign transaction (custodial) |

Integrates with Payment Service for fiat balance sync.

## Run

```bash
psql -U postgres -c "CREATE DATABASE wallet_service;"
./mvnw spring-boot:run
```

Swagger: http://localhost:8090/api/swagger-ui.html

See [docs/rules/wallet-service.md](../docs/rules/wallet-service.md).
