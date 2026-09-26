# Local Hardhat Demo — Invest → Token → Dividend

End-to-end on-chain flow on a local Hardhat node (chain ID 31337).

**Prerequisites:** PostgreSQL, Java 21, Node.js, Kafka (`docker compose --profile kafka up` from repo root when using event-driven settlement).

---

## 1. Start Hardhat node

```bash
cd token-issuance-service/hardhat
npm install
npm run node
```

Leave running on `http://localhost:8545`.

---

## 2. Deploy contracts

In a second terminal:

```bash
cd token-issuance-service/hardhat
npm run deploy:local
```

This deploys:

| Contract | Purpose |
|----------|---------|
| `ComplianceRegistry` | KYC whitelist on-chain |
| `MockUSDC` | 6-decimal USDC for Payment payouts |
| Demo `PropertyToken` | Optional smoke-test token |

Also:

- Whitelists Hardhat accounts **#0–#2**
- Mints 1M MockUSDC to operator (#0)
- Enables transfers on demo token
- Writes `hardhat/deployments/localhost.json`

Export addresses for Spring services:

```bash
eval "$(./scripts/export-env.sh)"
```

---

## 3. Dev wallet reference (Hardhat defaults)

| Account | Address | Role in demo |
|---------|---------|--------------|
| #0 | `0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266` | Operator, SPV wallet, payout signer |
| #1 | `0x70997970C51812dc3A010C724d1AfE6Fc599aa84` | Investor / buyer |
| #2 | `0x3C44CdDdB6a8fa426Eb90476d3413321120A0A01` | Second investor |

Use account **#0** as `spvWalletAddress` when issuing tokens. Register investor wallets in Compliance Service and link via Auth `PATCH /v1/users/me/wallet`.

Private key for #0 (public dev key): `0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80`

---

## 4. Start platform services

```bash
# From repo root — install shared libs once
./token-realty-app/mvnw -pl tokenrealty-security,tokenrealty-web,tokenrealty-jpa,tokenrealty-kafka,tokenrealty-events,tokenrealty-outbox install -DskipTests

# Export chain addresses (step 2)
eval "$(token-issuance-service/hardhat/scripts/export-env.sh)"

# Start with local profile where applicable
cd token-issuance-service && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
cd payment-service && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
# + Registry, Auth, Marketplace, Compliance, Kafka consumers as needed
```

**Issuance** (`local` profile): Hardhat RPC + `COMPLIANCE_REGISTRY_ADDRESS`.

**Payment** (`local` profile): `PAYMENT_BLOCKCHAIN_ENABLED=true`, MockUSDC address, auto-confirm for escrow (simulated payment tx; real USDC dividend payouts).

---

## 5. Happy-path flow

```text
1. Tokenize flat
   POST /v1/tokens — spvWalletAddress = Hardhat #0
   → deployFlat.js deploys PropertyToken, enables transfers
   → Registry PATCH token-info

2. KYC investor
   POST /v1/compliance + PATCH verify
   → kyc-approved → on-chain whitelist (ComplianceRegistry)

3. Primary buy
   POST /v1/listings + POST /v1/orders
   → Payment escrow (auto-confirm in local profile)
   → payment.confirmed (Kafka)
   → Issuance operatorTransfer(SPV → buyer) on PropertyToken
   → transfer.completed → escrow release

4. Dividend
   POST /v1/dividends/distribute (or rent.collected pipeline)
   → dividend.distributed (Kafka)
   → Payment PayoutService sends MockUSDC on-chain to holders
```

---

## 6. On-chain transfer model (MVP)

`PropertyToken` mints to the SPV wallet. Transfers use **`operatorTransfer(from, to, amount)`** signed by the operator — the SPV does not hold a signing key in MVP.

Requirements for a successful transfer:

1. `COMPLIANCE_REGISTRY_ADDRESS` configured in Issuance
2. Both `from` and `to` whitelisted on ComplianceRegistry
3. `transfersEnabled == true` (auto-enabled after deploy)

---

## 7. Troubleshooting

| Symptom | Fix |
|---------|-----|
| Deploy fails: compliance registry empty | Run `npm run deploy:local`; export `COMPLIANCE_REGISTRY_ADDRESS` |
| Transfer reverts: not whitelisted | KYC approve investor; wait for `kyc-approved` consumer or whitelist manually in deploy script |
| Transfer reverts: transfers disabled | Re-issue token or call `PATCH /v1/tokens/{id}/enable-transfers` |
| Dividend payout fails | Ensure operator has MockUSDC (`npm run deploy:local` mints 1M to #0) |
| Hardhat dir not found from Issuance | Set `HARDHAT_DIR=/path/to/token-issuance-service/hardhat` or run Issuance from service directory |

---

## Related docs

- [BUSINESS_RULES.md](BUSINESS_RULES.md) — buy flow order
- [EVENTS.md](EVENTS.md) — Kafka topics
- [PLATFORM-SPEC.md](PLATFORM-SPEC.md) §9 — foundation checklist
