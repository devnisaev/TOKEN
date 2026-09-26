---
name: web3j-blockchain-integration
description: Guide Web3j integration, RPC communication, transaction handling, and smart contract interaction inside Spring Boot microservices. Use when working on Token Issuance or Payment Service blockchain connectors.
---

# Web3j Blockchain Integration

Manage reliable communication between Spring Boot services and EVM networks (Polygon / Hardhat).

## Core Rules

1. **Wallet & Key Management:** Never hardcode private keys in application code. Load credentials securely via environment variables or secret managers.
2. **Transaction Receipts:** Always wait for transaction confirmation receipts (`transactionReceiptProcessor`) and check status codes before marking payments or token deployments successful.
3. **Gas Estimation:** Implement dynamic gas fee calculations rather than hardcoding gas limits to prevent out-of-gas failures during network congestion.
4. **Error Handling:** Catch Web3j contract call exceptions cleanly and map them to domain-specific business errors (e.g., insufficient funds, reverted transaction).

## Forbidden

- Assuming a transaction succeeded immediately after submission without checking the mined receipt block.
- Blocking core REST threads indefinitely on synchronous RPC network calls without timeouts.

---

## Project layout

| Service | Class | Role |
|---------|-------|------|
| Token Issuance | `blockchain/BlockchainConnector.java` | Raw Web3j: send tx, wait receipt, ABI encoders |
| Token Issuance | `blockchain/ContractDeployer.java` | Spawns Hardhat `deployFlat.js` |
| Token Issuance | `service/TransferService.java` | `operatorTransfer` on PropertyToken |
| Token Issuance | `service/OnChainWhitelistService.java` | ComplianceRegistry add/remove |
| Token Issuance | `config/Web3jConfig.java`, `BlockchainProperties.java` | RPC, credentials, gas |
| Payment | `blockchain/PaymentBlockchainService.java` | MockUSDC/USDC transfer + receipt lookup |
| Payment | `service/PaymentService.java` | Validates receipt on confirm when blockchain enabled |
| Payment | `service/PayoutService.java` | On-chain dividend/rent payouts |

Hardhat contracts: `token-issuance-service/hardhat/contracts/`

Runbook: [docs/hardhat-demo.md](../../../docs/hardhat-demo.md)

## Configuration (env only)

**Issuance** (`application.yml` / `local` profile):

```yaml
blockchain:
  rpc-url: ${BLOCKCHAIN_RPC_URL:http://localhost:8545}
  chain-id: ${BLOCKCHAIN_CHAIN_ID:31337}
  operator-private-key: ${OPERATOR_PRIVATE_KEY}   # never commit real keys
  compliance-registry-address: ${COMPLIANCE_REGISTRY_ADDRESS:}
```

**Payment**:

```yaml
tokenrealty.payment.blockchain:
  enabled: ${PAYMENT_BLOCKCHAIN_ENABLED:false}
  rpc-url: ${BLOCKCHAIN_RPC_URL:http://localhost:8545}
  operator-private-key: ${OPERATOR_PRIVATE_KEY:}
  usdc-contract-address: ${USDC_CONTRACT_ADDRESS:}
```

After `npm run deploy:local`:

```bash
eval "$(token-issuance-service/hardhat/scripts/export-env.sh)"
```

Dev Hardhat account #0 key is public knowledge — local only.

## Transaction patterns (implemented)

### Submit + wait (Issuance transfers)

```text
1. sendContractTransaction(contract, encodedData, valueWei) → txHash
2. waitForReceipt(txHash) — poll up to ~120s
3. isTransactionSuccessful(receipt) — status 0x1
4. Update DB holder balances only after confirmed receipt
```

Use **`operatorTransfer(from, to, amount)`** (selector `0x0d1af103`) when moving tokens from SPV wallet — operator signs; SPV has no service key.

### Payment confirm

- `PaymentBlockchainService.findReceipt(txHash)` — skips `0xSIMULATED_*` hashes
- When `PAYMENT_BLOCKCHAIN_ENABLED=true`, `PaymentService.confirm()` rejects unconfirmed txs
- **Local demo:** auto-confirm uses simulated payment tx; real USDC payouts when blockchain enabled on `PayoutService`

### Deploy (Hardhat subprocess)

`ContractDeployer` runs `npx hardhat run scripts/deployFlat.js` — not pure Web3j. Set `HARDHAT_DIR` if CWD is not `token-issuance-service/`.

Post-deploy: Issuance auto-calls `enableTransfers()` on-chain.

## ABI encoding (MVP)

Manual encoders in `BlockchainConnector` — no generated wrappers yet.

| Function | Selector / method |
|----------|-------------------|
| `enableTransfers()` | `encodeEnableTransfers()` |
| `operatorTransfer(from,to,amt)` | `encodeOperatorTransfer()` |
| `isWhitelisted(address)` | `encodeIsWhitelisted()` |
| ERC-20 `transfer(to, amt)` | `PaymentBlockchainService` |

When changing contract ABIs: update encoders or run Web3j codegen (`hardhat/package.json` → `generate-wrappers` — script TBD).

## Error mapping

| On-chain / Web3j failure | Map to |
|--------------------------|--------|
| Reverted transfer (not whitelisted, transfers disabled) | Log + `TransferStatus.FAILED`; do not update balances |
| RPC timeout / connection refused | `ValidationException` ("blockchain unavailable") at service boundary |
| Insufficient USDC for payout | Catch in `PaymentBlockchainService`; fail payout row |
| Compliance not configured | `OnChainWhitelistService` returns `0xNOT_CONFIGURED` — log, no throw |

Do not return HTTP 500 for expected revert reasons — use 422 / domain status on REST paths.

## Workflow — add new on-chain action

1. Add Solidity function + compile Hardhat.
2. Add encoder in `BlockchainConnector` or dedicated service.
3. Call from `@Transactional` service method — **do not** hold DB TX open during `waitForReceipt` if call exceeds a few seconds; consider async job for production.
4. Update `docs/hardhat-demo.md` and env vars in deploy script.
5. Test on local Hardhat node with whitelisted wallets.

## On-chain prerequisites (local demo)

1. Hardhat node on `:8545`
2. `npm run deploy:local` → ComplianceRegistry + MockUSDC addresses
3. Whitelist buyer + SPV wallets (deploy script whitelists #0–#2)
4. Issue token with `spvWalletAddress` = Hardhat #0
5. KYC approve investor → `kyc-approved` → on-chain whitelist (or deploy script)

## Related docs

- [.cursor/skills/solidity-smart-contract/SKILL.md](../solidity-smart-contract/SKILL.md) — contract side
- [docs/hardhat-demo.md](../../../docs/hardhat-demo.md) — E2E flow
- [.cursor/rules/payment-ledger.mdc](../../rules/payment-ledger.mdc) — escrow + txHash traceability
- [docs/BUSINESS_RULES.md](../../../docs/BUSINESS_RULES.md) — no private keys in logs/events
- [docs/PLATFORM-SPEC.md](../../../docs/PLATFORM-SPEC.md) §9.1, §9.4 — contract & dividend backlog
