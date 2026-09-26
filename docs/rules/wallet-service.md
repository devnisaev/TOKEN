# Wallet Service — TokenRealty

**Status:** MVP implemented in `wallet-service/` (port **8090**, DB `wallet_service`).

Investor wallet custody and linking — complements Auth `User.walletAddress` (profile field) with persisted wallet records, encrypted custodial keys, and aggregate balance views.

Related: [web3j-blockchain-integration SKILL](../../.cursor/skills/web3j-blockchain-integration/SKILL.md) · [payment-ledger.md](payment-ledger.md) · [rest-client-errors.md](rest-client-errors.md)

---

## Responsibilities

| Area | Owner | Notes |
|------|-------|-------|
| Custodial key generation | Wallet Service | Web3j `Keys.createEcKeyPair()` |
| Key encryption at rest | Wallet Service | AES-GCM via `WalletEncryptionService` |
| External wallet linking | Wallet Service | MetaMask / WalletConnect address registration |
| Fiat/stablecoin balance | Payment Service | `GET /v1/wallet-balances/{investorId}` |
| Token holdings | Token Issuance | `GET /v1/investors/{investorId}/holdings` |
| On-chain signing (custodial) | Wallet Service | `POST /v1/wallets/{investorId}/sign` |
| KYC / whitelist | Compliance + Issuance | Wallet Service does **not** replace compliance gates |

---

## API

| Method | Path | Role | Description |
|--------|------|------|-------------|
| `POST` | `/v1/wallets` | `INVESTOR`, `ADMIN` | Create custodial wallet (one per investor) |
| `POST` | `/v1/wallets/link` | `INVESTOR`, `ADMIN` | Link external wallet address |
| `POST` | `/v1/wallets/connect-session` | `INVESTOR`, `ADMIN` | WalletConnect v2 session stub (relay topic + URI) |
| `GET` | `/v1/wallets/{investorId}` | `INVESTOR`, `ADMIN`, `SERVICE` | List wallets |
| `GET` | `/v1/wallets/{investorId}/balance` | `INVESTOR`, `ADMIN`, `SERVICE` | Aggregate Payment + Issuance balances |
| `POST` | `/v1/wallets/{investorId}/sign` | `INVESTOR`, `ADMIN` | Sign raw transaction (custodial only) |

Gateway route: `http://localhost:8080/api/v1/wallets/**` → `:8090`.

---

## Security invariants

- **Never** log, expose in API responses, or publish to Kafka: private keys, seeds, or `encryptedPrivateKey` column values.
- **`WALLET_ENCRYPTION_KEY`** — min 32 characters; rotate via env/secret manager in production (re-encrypt migration TBD).
- **Access control** — investors may only access their own `investorId` (`TokenPrincipal.userId`); `ADMIN` / `SERVICE` bypass for ops and inter-service calls.
- **Custodial limit** — one `CUSTODIAL` wallet per investor; multiple `LINKED` wallets allowed.
- **Address uniqueness** — global unique index on `wallet_address` (lowercase normalized).

---

## Package layout

```
wallet-service/src/main/java/com/tokenrealty/wallet/
├── controller/WalletController.java
├── service/WalletService.java, CustodialSignService.java, WalletAccessGuard.java
├── crypto/WalletEncryptionService.java          ← AES-GCM encrypt/decrypt
├── entity/InvestorWallet.java
├── repository/InvestorWalletRepository.java
├── client/PaymentClient.java, IssuanceClient.java
└── config/SecurityConfig.java, ServiceClientConfig.java
```

---

## Configuration

```yaml
# wallet-service application.yml
tokenrealty:
  wallet:
    encryption-key: ${WALLET_ENCRYPTION_KEY}   # required, min 32 chars
    blockchain:
      rpc-url: ${BLOCKCHAIN_RPC_URL:http://localhost:8545}
      chain-id: ${BLOCKCHAIN_CHAIN_ID:31337}
  service-account:
    client-id: wallet
    client-secret: ${SERVICE_ACCOUNT_SECRET:wallet-secret}

services:
  payment:
    url: ${PAYMENT_SERVICE_URL:http://localhost:8085/api}
  token-issuance:
    url: ${TOKEN_ISSUANCE_URL:http://localhost:8082/api}
```

Auth dev seed: service account `wallet` / `wallet-secret` (ADMIN role).

---

## Aggregate balance flow

```text
GET /v1/wallets/{investorId}/balance
  ├─ PaymentClient  → GET /v1/wallet-balances/{investorId}   (USDC available/held)
  └─ IssuanceClient → GET /v1/investors/{investorId}/holdings (token positions)
```

Returns `AggregateBalanceResponse`: primary wallet address, fiat balances, token holdings list.

---

## Custodial signing

`CustodialSignService` decrypts the stored key in-process, builds a Web3j `RawTransaction`, and returns a signed hex blob. Caller broadcasts via RPC or Payment/Issuance blockchain connectors.

**Do not** hold open transactions around decrypt/sign — read-only TX on wallet lookup only.

---

## Pending / future

- [x] WalletConnect session handshake (`POST /v1/wallets/connect-session` — relay URL + TTL)
- [x] Key rotation and re-encryption migration (`POST /v1/wallets/{investorId}/rotate-encryption`)
- [ ] HSM / KMS-backed encryption (replace env symmetric key)
- [x] Payment Service syncs `WalletBalance` on initiate/confirm/payout (see [payment-ledger.md](payment-ledger.md))
- [x] Rate limits on `/sign` endpoint (per-investor in-memory, 10/min default)
