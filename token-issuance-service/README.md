# Token Issuance Service

Microservice for **TokenRealty** — deploys and manages ERC-1400 property tokens on Polygon.

## Architecture

```
REST API (:8082)
    ↓
Services
    TokenIssuanceService   → deploys PropertyToken.sol via Hardhat subprocess
    TransferService        → investor-to-investor transfers with compliance check
    DividendService        → rental income distribution, monthly scheduled job
    ComplianceService      → KYC whitelist (DB + on-chain ComplianceRegistry)
    ↓
BlockchainConnector (Web3j)
    ↓
Polygon Amoy testnet / Local Hardhat node
```

## Quick start

### Prerequisites
- Java 21
- PostgreSQL running (`property_registry` and `token_issuance` databases)
- Hardhat node running (for local blockchain)

### 1. Start Hardhat node
```bash
cd hardhat
npm install
npm run node          # starts at localhost:8545
npm run deploy:local  # deploys contracts, copy addresses to application.yml
```

### 2. Create database
```bash
psql -U postgres -c "CREATE DATABASE token_issuance;"
```

### 3. Run the service
```bash
# Local profile (Hardhat node)
./mvnw spring-boot:run -DskipTests -Dspring.profiles.active=local

# Staging profile (Polygon Amoy)
OPERATOR_PRIVATE_KEY=0x... POLYGON_AMOY_RPC_URL=https://... \
  ./mvnw spring-boot:run -DskipTests -Dspring.profiles.active=staging
```

### 4. Swagger UI
```
http://localhost:8082/api/swagger-ui.html
```

## API Overview

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/v1/tokens` | ADMIN | Issue tokens for a flat |
| GET | `/v1/tokens/{id}` | Any | Get token contract |
| GET | `/v1/tokens/by-flat/{flatId}` | Any | Get contract by flat |
| PATCH | `/v1/tokens/{id}/enable-transfers` | ADMIN | Enable trading |
| PATCH | `/v1/tokens/{id}/suspend-transfers` | ADMIN | Halt trading |
| GET | `/v1/tokens/{id}/holders` | Any | List token holders |
| POST | `/v1/tokens/{id}/transfers` | ADMIN | Execute transfer |
| POST | `/v1/tokens/{id}/dividends/distribute` | ADMIN | Distribute rental income |
| GET | `/v1/compliance` | ADMIN/COMPLIANCE | List KYC records |
| POST | `/v1/compliance` | ADMIN/COMPLIANCE | Register investor |
| PATCH | `/v1/compliance/{id}/verify` | ADMIN/COMPLIANCE | Verify KYC + whitelist |
| GET | `/v1/compliance/check/{wallet}` | Any | Check wallet whitelist status |

## Authentication

JWT Bearer from Auth Service (:8083). Inter-service calls to Property Registry use service account `token-issuance` / `issuance-secret` via `ServiceTokenProvider`.

## Token lifecycle

```
1. Flat registered in Property Registry (status: AVAILABLE)
2. SPV registered + KYC verified
3. POST /v1/tokens → deploys PropertyToken.sol on Polygon
4. Flat status updated to TOKENIZED in Property Registry
5. Investors registered + KYC verified in ComplianceService
6. PATCH /v1/tokens/{id}/enable-transfers → trading opens
7. Investors buy tokens via POST /v1/tokens/{id}/transfers
8. Monthly: POST /v1/tokens/{id}/dividends/distribute
```