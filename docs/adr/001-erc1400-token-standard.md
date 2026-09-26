# ADR 001: ERC-1400 Security Token Standard

**Status:** Accepted (2026-09-26)  
**Context:** TokenRealty property tokens require transfer restrictions aligned with KYC whitelist.

## Decision

Continue with **ERC-1400–style partitioned tokens** (`PropertyToken.sol`) with off-chain compliance gates and on-chain whitelist hooks via `ComplianceRegistry`.

## Rationale

- Supports partial restrictions per partition (primary vs secondary).
- Matches existing Hardhat contracts and issuance service integration.
- ERC-20 + external hook would require contract migration for little gain at current scale.

## Consequences

- Issuance and Payment services remain Web3j-first against ERC-1400 ABI.
- Secondary market settlement still requires compliance check before match (off-chain) and whitelist before transfer (on-chain).
