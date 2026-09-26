---
name: rwa-legal-compliance
description: Ensure database schemas, API contracts, and business logic respect Real-World Asset (RWA) regulations. Use when building compliance, tenant, registry, or document services.
---

# RWA Legal & Compliance Guidelines

Enforce strict real estate and securities regulations across TokenRealty backend components.

## Core Rules

1. **KYC/AML Gates:** Every buy order, secondary market transfer, and token issuance action must verify wallet compliance state via the `ComplianceService` or on-chain registry.
2. **SPV Data Integrity:** Property registry models must accurately capture legal structures (`spvRegistrationNumber`, legal jurisdiction, and title deed identifiers).
3. **IPFS Immutability:** Legal paperwork (title deeds, appraisals, articles of incorporation) must be pinned to IPFS, storing immutable CIDs in the database.
4. **Jurisdiction Restrictions:** Ensure investor country-of-residence codes are validated against property-level regulatory allowances before completing trades.

---

## Evaluation order (never skip)

From [docs/BUSINESS_RULES.md](../../../docs/BUSINESS_RULES.md) and [investment-limits.mdc](../../rules/investment-limits.mdc):

```text
1. Listing active + tokensAvailable
2. tokenAmount >= minInvestmentTokens
3. Compliance check (buyer wallet) — before matched order / escrow
4. Secondary: seller KYC + holder balance
5. Payment escrow → on-chain transfer → settle
6. Primary sell-out → Registry FULLY_SOLD
```

KYC must pass **before** money moves. Escrow release only after `transfer.completed`.

## Service responsibilities

| Concern | Service | Port | Mechanism |
|---------|---------|------|-----------|
| KYC register / verify / revoke | Compliance (:8087) | REST | `POST /v1/compliance`, `PATCH verify/revoke` |
| Wallet whitelist check | Compliance | REST | `GET /v1/compliance/check/{wallet}` |
| On-chain whitelist sync | Token Issuance | Kafka | `kyc-approved` / `kyc-revoked` → `OnChainWhitelistService` |
| Buy/sell KYC gate | Marketplace | RestClient | `ComplianceClient.isWalletApproved()` |
| Transfer KYC gate | Token Issuance | RestClient | `ComplianceClient` before on-chain transfer |
| Legal entity metadata | Property Registry | REST | `SpvEntity`, `Building`, `Flat`, `Valuation` |
| Document data room | Document (:8088) | REST | IPFS pin → Registry `PropertyDocument.ipfsCid` |
| Document verify | Property Registry | REST | `PATCH /v1/documents/{id}/verify` (Compliance role) |
| Investor notification | Notification (:8089) | Kafka | `kyc-approved`, `kyc-revoked`, etc. |

## Registry — required legal / physical fields

See PLATFORM-SPEC §10.8b. Key entities:

| Entity | Compliance-relevant fields |
|--------|---------------------------|
| `SpvEntity` | `legalName`, `registrationNumber`, `registrationCountry`, `legalJurisdiction`, `ownershipType`, `kycVerified` |
| `Building` | `propertyCategory`, `cadastralReference`, `constructionYear` |
| `Flat` | `cadastralReference`, `netUsableAreaSqm`, `status` (`AVAILABLE` → `TOKENIZED` → `FULLY_SOLD`) |
| `PropertyDocument` | `documentType` (TITLE_DEED, VALUATION_REPORT, ARTICLES_OF_INCORPORATION, …), `ipfsCid`, `isVerified` |
| `Valuation` | `valueUsd`, `appraiserName`, `operatingExpensesEstimateUsd`, `targetRentalYieldPct` |

Do **not** add per-document `*IpfsCid` columns on Building/Flat — use `PropertyDocument` + Document Service upload.

## Document / data room workflow

```text
1. POST /v1/documents/upload (Document Service) — multipart → IPFS CID
2. Callback POST /v1/buildings/{id}/documents or /v1/flats/{id}/documents (Registry)
3. Outbox document.uploaded event
4. Compliance PATCH /v1/documents/{id}/verify when reviewed
```

Dev: simulated CID (`IPFS_MODE=simulated`); prod: Pinata (`IPFS_MODE=pinata`, `PINATA_JWT`).

## KYC / on-chain sync workflow

```text
1. POST /v1/compliance — register investor + wallet
2. PATCH /v1/compliance/{id}/verify → kyc-approved (outbox)
3. Issuance KycApprovedListener → ComplianceRegistry.addToWhitelist
4. Marketplace buy → ComplianceClient check (off-chain mirror)
5. PropertyToken transfer → isWhitelisted(from) && isWhitelisted(to)
```

Revoke path: `kyc-revoked` → `removeFromWhitelist`.

## Jurisdiction restrictions (future / partial)

Not fully implemented — when adding:

- Store `countryCode` on `ComplianceRecord` (Compliance Service)
- Add property-level `allowedInvestorCountries` or jurisdiction rules on `Building` / `SpvEntity`
- Validate in Marketplace **before** order match; fail with `ValidationException` / 422
- Do not hardcode bypass in production paths

Until policy service exists, document assumptions in API and use Compliance `countryCode` from KYC events.

## Forbidden (platform)

- Skipping KYC check on buy, sell, or primary transfer paths
- Storing KYC document **content** in Kafka payloads or logs — reference document ID / CID only
- Mutating `PropertyDocument.ipfsCid` after verify — upload new document instead
- Assigning flat `FULLY_SOLD` on secondary listings (primary sell-out only)
- Private keys or seeds in API responses, events, or registry fields

## Related docs

- [docs/BUSINESS_RULES.md](../../../docs/BUSINESS_RULES.md) — tier-1 invariants
- [.cursor/rules/investment-limits.mdc](../../rules/investment-limits.mdc) — KYC gates, min investment
- [docs/EVENTS.md](../../../docs/EVENTS.md) — `kyc-approved`, `document.uploaded`
- [docs/hardhat-demo.md](../../../docs/hardhat-demo.md) — on-chain whitelist demo
- [docs/PLATFORM-SPEC.md](../../../docs/PLATFORM-SPEC.md) §10.7–§10.8 — Compliance & Document checklists
