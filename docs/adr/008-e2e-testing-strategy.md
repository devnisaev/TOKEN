# ADR 008: E2E Testing Strategy

**Status:** Accepted (2026-09-26)

## Decision

Three-tier Playwright strategy:

| Tier | When | Scope |
|------|------|--------|
| **Smoke** | Every PR (`frontend-e2e-smoke`) | Auth shell, routing, no live gateway |
| **Compose subset** | Every PR (`compose-e2e-pr`) | 16 gateway-backed specs via `ci-compose-e2e.sh` |
| **Nightly + Hardhat** | Schedule (`nightly-e2e.yml`) | Same 16 specs with `CI_E2E_HARDHAT=true` |

Local: `./scripts/e2e-run.sh --compose-subset` or `--compose-subset --hardhat` against a running demo stack.

## Rationale

- PR CI stays under ~60 minutes without Hardhat node startup.
- Hardhat-dependent specs (buy-settled, tokenize) run nightly with seeded contracts.
- Compose subset mirrors CI so developers reproduce failures locally.

## Consequences

- Hardhat specs may skip in PR compose job when `CI_E2E_HARDHAT` is unset.
- Nightly workflow is the gate for on-chain E2E regressions.
