# Blockchain Indexer

On-chain event polling and balance reconciliation for **TokenRealty**.

Port **8091** · Database **`blockchain_indexer`**

## API

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| GET | `/v1/indexer/status` | ADMIN | Poll cursor, last block, contract count |
| GET | `/v1/indexer/events` | ADMIN | Recent indexed on-chain events |
| GET | `/v1/indexer/reconciliation` | ADMIN | Balance mismatch report |

## Features

- Polls Hardhat/Polygon for Transfer, WhitelistAdded, WhitelistRemoved
- Persists indexed events to DB
- Scheduled reconciliation: on-chain vs holder registry
- Publishes `transfer.indexed`, `balance.mismatch` via outbox
- Prometheus metrics: `indexer.block.lag`, `indexer.events.indexed.total`, `indexer.balance.mismatches.open` (`/actuator/prometheus`)

## Run

```bash
psql -U postgres -c "CREATE DATABASE blockchain_indexer;"
./mvnw spring-boot:run
```

Requires RPC URL (`BLOCKCHAIN_RPC_URL`). See [docs/rules/blockchain-indexer.md](../docs/rules/blockchain-indexer.md).
