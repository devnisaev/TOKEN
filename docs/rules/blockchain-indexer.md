# Blockchain Indexer — TokenRealty

**Status:** MVP implemented in `blockchain-indexer-service/` (port **8091**, DB `blockchain_indexer`).

Standalone service that polls an EVM node (Hardhat local / Polygon) for property-token and compliance-registry events, persists an audit log, and reconciles on-chain ERC-20 balances against Token Issuance holder records.

Related: [web3j-blockchain-integration SKILL](../../.cursor/skills/web3j-blockchain-integration/SKILL.md) · [hardhat-demo.md](../hardhat-demo.md) · [payment-ledger.md](payment-ledger.md)

---

## Responsibilities

| Area | Owner | Notes |
|------|-------|-------|
| Event log ingestion | Blockchain Indexer | Poll `eth_getLogs` — no WebSocket yet |
| Transfer events | Indexer | ERC-20 `Transfer(address,address,uint256)` on PropertyToken contracts |
| Whitelist events | Indexer | `WhitelistAdded`, `WhitelistRemoved` on ComplianceRegistry |
| Balance reconciliation | Indexer | `balanceOf` vs Issuance `TokenHolder.balance` |
| Holder authority | Token Issuance | Indexer reads via REST; does **not** mutate Issuance DB |
| Payment tx reconciliation | Payment Service | Separate job in Payment (`PaymentBlockchainReconciliationWorker`) |

---

## Indexed event types

| On-chain event | Stored `eventType` | Source contract |
|----------------|-------------------|-----------------|
| `Transfer(address,address,uint256)` | `Transfer` | PropertyToken (each tracked address) |
| `WhitelistAdded(address,string,uint256)` | `WhitelistAdded` | ComplianceRegistry |
| `WhitelistRemoved(address)` | `WhitelistRemoved` | ComplianceRegistry |

Payload stored as JSON (`topic0`, `topic1`, `topic2`, `data`) in `indexed_events` table. Dedupe key: `(tx_hash, log_index)`.

---

## API (admin / service)

| Method | Path | Role | Description |
|--------|------|------|-------------|
| `GET` | `/v1/indexer/status` | `ADMIN`, `SERVICE` | Last processed block number |
| `GET` | `/v1/indexer/events` | `ADMIN`, `SERVICE` | Paginated indexed events |
| `GET` | `/v1/indexer/reconciliation` | `ADMIN`, `SERVICE` | Open balance mismatches |

Gateway route: `http://localhost:8080/api/v1/indexer/**` → `:8091`.

---

## Scheduled jobs

| Job | Schedule | Class | When disabled |
|-----|----------|-------|---------------|
| Log poll | `${INDEXER_POLL_MS:15000}` fixed delay | `BlockchainLogIndexer` | `INDEXER_ENABLED=false` |
| Balance reconciliation | `${INDEXER_RECONCILIATION_CRON:0 30 */6 * * *}` | `BalanceReconciliationService` | skips when `INDEXER_ENABLED=false` |

Poll batch size: `${INDEXER_BATCH_BLOCKS:500}` blocks per cycle.

---

## Tracked contracts

1. **PropertyToken addresses** — fetched from Issuance `GET /v1/tokens?size=100` (`contractAddress` field).
2. **ComplianceRegistry** — `COMPLIANCE_REGISTRY_ADDRESS` env (from Hardhat `deploy:local` / `export-env.sh`).

Cursor stored in `indexer_cursors` (`id = main`, `last_block_number`).

---

## Reconciliation

For each active Token Issuance contract with a deployed address:

```text
For each TokenHolder (balance > 0):
  chainBalance = ERC20.balanceOf(wallet) via Web3j eth_call
  if chainBalance != holder.balance:
    insert reconciliation_mismatches row + WARN log
```

Mismatches are **observability only** in MVP — no auto-correction. Ops uses `GET /v1/indexer/reconciliation` or logs.

---

## Package layout

```
blockchain-indexer-service/src/main/java/com/tokenrealty/indexer/
├── controller/IndexerController.java
├── service/BlockchainLogIndexer.java, BalanceReconciliationService.java
├── blockchain/EventTopics.java, OnChainBalanceReader.java
├── entity/IndexerCursor.java, IndexedEvent.java, ReconciliationMismatch.java
├── repository/*Repository.java
├── client/IssuanceClient.java
└── config/IndexerBlockchainConfig.java, SecurityConfig.java, ServiceClientConfig.java
```

---

## Configuration

```yaml
# blockchain-indexer-service application.yml
tokenrealty:
  indexer:
    enabled: ${INDEXER_ENABLED:true}
    poll-ms: ${INDEXER_POLL_MS:15000}
    batch-blocks: ${INDEXER_BATCH_BLOCKS:500}
    reconciliation:
      cron: ${INDEXER_RECONCILIATION_CRON:0 30 */6 * * *}
    blockchain:
      rpc-url: ${BLOCKCHAIN_RPC_URL:http://localhost:8545}
      compliance-registry-address: ${COMPLIANCE_REGISTRY_ADDRESS:}
  service-account:
    client-id: blockchain-indexer
    client-secret: ${SERVICE_ACCOUNT_SECRET:indexer-secret}

services:
  token-issuance:
    url: ${TOKEN_ISSUANCE_URL:http://localhost:8082/api}
```

Auth dev seed: service account `blockchain-indexer` / `indexer-secret` (ADMIN role).

**Prerequisites:** Hardhat node running (`npm run node` in `token-issuance-service/hardhat`) and contracts deployed (`npm run deploy:local`).

---

## Security invariants

- Indexer is **read-only** on-chain (eth_call + eth_getLogs only).
- No private keys in indexer config or DB.
- Admin/service JWT required for all `/v1/indexer/**` endpoints.

---

## Kafka (outbox)

When `KAFKA_ENABLED=true`:

| Topic | Publisher | Payload |
|-------|-----------|---------|
| `tokenrealty.indexer.transfer.indexed.v1` | `OutboxIndexedEventPublisher` | `indexedEventId`, `eventType`, `contractAddress`, `txHash`, `logIndex`, `blockNumber` |
| `tokenrealty.indexer.balance.mismatch.v1` | `OutboxBalanceMismatchPublisher` | `mismatchId`, `contractId`, `walletAddress`, `dbBalance`, `chainBalance` |

Relay: `OutboxRelayWorker` (same pattern as Payment Service).

## Pending / future

- [ ] WebSocket / eth_subscribe instead of polling
- [ ] Auto-remediation workflow (Issuance holder balance sync with approval)
- [ ] DividendPaid / custom PropertyToken events
- [ ] Metrics: lag blocks, mismatch count (Micrometer)
