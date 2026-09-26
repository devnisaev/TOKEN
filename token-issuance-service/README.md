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
| GET | `/v1/tokens` | Any | List token contracts |
| POST | `/v1/tokens` | ADMIN | Issue tokens for a flat (deploy on-chain) |
| GET | `/v1/tokens/{id}` | Any | Get token contract |
| GET | `/v1/tokens/by-flat/{flatId}` | Any | Get contract by flat |
| PATCH | `/v1/tokens/{id}/enable-transfers` | ADMIN | Enable trading |
| PATCH | `/v1/tokens/{id}/suspend-transfers` | ADMIN | Halt trading |
| GET | `/v1/tokens/{contractId}/holders` | Any | List token holders |
| GET | `/v1/tokens/{contractId}/holders/{holderId}` | Any | Get holder by id |
| GET | `/v1/tokens/{contractId}/holders/by-wallet/{walletAddress}` | Any | Lookup holder by wallet |
| POST | `/v1/tokens/{contractId}/holders` | ADMIN | Register holder |
| GET | `/v1/tokens/{contractId}/transfers` | Any | List transfers |
| GET | `/v1/tokens/{contractId}/transfers/{id}` | Any | Get transfer |
| POST | `/v1/tokens/{contractId}/transfers` | ADMIN | Execute on-chain transfer |
| GET | `/v1/tokens/{contractId}/dividends` | Any | Dividend history for contract |
| GET | `/v1/investors/{investorId}/dividends` | Any | Investor dividend history |
| POST | `/v1/tokens/{contractId}/dividends/distribute` | ADMIN | Distribute rental income |
| GET | `/v1/investors/{investorId}/holdings` | Any | Token holdings for investor |

KYC whitelist APIs live in **Compliance Service** (:8087) — Issuance calls `ComplianceClient` internally.

## Authentication

JWT Bearer from Auth Service (:8083). Inter-service calls to Property Registry use service account `token-issuance` / `issuance-secret` via `ServiceTokenProvider`.

## Token lifecycle

```
1. Flat registered in Property Registry (status: AVAILABLE)
2. SPV registered + KYC verified
3. POST /v1/tokens → deploys PropertyToken.sol on Polygon
4. Flat status updated to TOKENIZED in Property Registry
5. Investors registered + KYC verified in Compliance Service (:8087)
6. PATCH /v1/tokens/{id}/enable-transfers → trading opens
7. Marketplace buy flow → Payment → POST /v1/tokens/{contractId}/transfers (operator transfer)
8. Monthly: POST /v1/tokens/{contractId}/dividends/distribute
```