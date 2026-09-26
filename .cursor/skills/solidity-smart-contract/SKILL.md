---
name: solidity-smart-contract
description: Guide Solidity smart contract development, Hardhat deployment scripts, and security patterns for TokenRealty. Use when writing or reviewing smart contracts like PropertyToken, ComplianceRegistry, or MockUSDC.
---

# Solidity Smart Contract Guidelines

Follow secure coding and upgradeability standards for RWA tokenization on EVM chains (Polygon / Hardhat).

## Core Rules

1. **Access Control:** Always use OpenZeppelin `AccessControl` or `Ownable2Step` for administrative functions (e.g., token minting, compliance whitelisting, pausing).
2. **Compliance Gating:** Ensure transfer functions (`transfer`, `transferFrom`) check the `ComplianceRegistry` contract before executing state changes.
3. **Reentrancy Protection:** Apply `ReentrancyGuard` on any contract handling multi-step escrow releases or dividend distributions.
4. **Hardhat Configuration:** Wire deployment scripts in `hardhat/scripts/` to output contract addresses dynamically into backend configuration templates.
5. **Testing:** Write comprehensive Hardhat / ethers.js tests covering edge cases (e.g., transfers blocked for unverified wallets, exact pro-rata dividend math).

## Forbidden

- Unprotected `selfdestruct` or `delegatecall` primitives.
- Hardcoded test private keys in production contract source files.
- Omitting custom error types in favor of generic string reverts (use custom errors for gas optimization).

---

## Project layout (TokenRealty)

| Path | Purpose |
|------|---------|
| `token-issuance-service/hardhat/contracts/PropertyToken.sol` | ERC-20 + compliance-gated `_update`; `operatorTransfer` for custodial SPV→buyer |
| `token-issuance-service/hardhat/contracts/ComplianceRegistry.sol` | On-chain KYC whitelist (`addToWhitelist`, `isWhitelisted`) |
| `token-issuance-service/hardhat/contracts/MockUSDC.sol` | 6-decimal USDC for local Payment payouts |
| `token-issuance-service/hardhat/scripts/deploy.js` | Bootstrap deploy → `deployments/localhost.json` |
| `token-issuance-service/hardhat/scripts/deployFlat.js` | Per-flat deploy (invoked by Java `ContractDeployer`) |
| `token-issuance-service/hardhat/scripts/export-env.sh` | Export addresses for Spring `local` profile |
| `docs/hardhat-demo.md` | End-to-end local on-chain demo runbook |

Compile and deploy:

```bash
cd token-issuance-service/hardhat
npm run compile
npm run node          # terminal 1
npm run deploy:local  # terminal 2
eval "$(./scripts/export-env.sh)"
```

## MVP conventions (current codebase)

- **PropertyToken** uses `Ownable` today; prefer **`Ownable2Step`** when adding new admin contracts or refactoring.
- Compliance is enforced in **`_update`** (covers `transfer`, `transferFrom`, and `operatorTransfer`), not in public transfer wrappers alone.
- **`transfersEnabled`** must be `true` before any wallet-to-wallet move; `deployFlat.js` and Issuance auto-enable after deploy.
- **`operatorTransfer(from, to, amount)`** — custodial path: operator signs for SPV wallet (MVP; SPV has no private key in services).
- **DividendDistributor.sol** is deferred; dividends flow off-chain via Payment Service MockUSDC until an on-chain distributor is requested.

## Workflow — new or changed contract

1. Add or edit contract under `hardhat/contracts/` (Solidity `^0.8.20`, OpenZeppelin imports).
2. Run `npm run compile`; fix warnings before merge.
3. Update `deploy.js` / `deployFlat.js` if constructor args or post-deploy steps change (whitelist, enable transfers, mint USDC).
4. If ABI selectors change, update Java encoders in `BlockchainConnector` / `PaymentBlockchainService` or generate Web3j wrappers.
5. Add Hardhat tests under `hardhat/test/` (currently missing — required for non-trivial logic).
6. Document env vars in `docs/hardhat-demo.md` and PLATFORM-SPEC §9.1.

## Workflow — security review checklist

- [ ] All state-changing admin functions have `onlyOwner` or role gate
- [ ] Transfer path checks `ComplianceRegistry.isWhitelisted` for both `from` and `to` (when not mint/burn)
- [ ] No unchecked external calls before state updates (reentrancy)
- [ ] Custom errors instead of long revert strings for new code
- [ ] No secrets in `.sol` files; operator key only in env / Spring config (dev Hardhat #0 is public)
- [ ] Deploy scripts write addresses to `deployments/localhost.json`, not hardcoded in Java

## Related project docs

- [docs/hardhat-demo.md](../../../docs/hardhat-demo.md) — local invest → token → dividend
- [docs/BUSINESS_RULES.md](../../../docs/BUSINESS_RULES.md) — platform invariants
- [docs/PLATFORM-SPEC.md](../../../docs/PLATFORM-SPEC.md) §9.1 — smart contract checklist
- [.cursor/rules/kafka-messaging.mdc](../../rules/kafka-messaging.mdc) — kyc-approved → on-chain whitelist sync
