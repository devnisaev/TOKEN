# Phase 11 — Institutional UI & BFF

**Status:** Planned (tracks 553–577). Exposes Phase 10 backend (governance, tax withholding, surveillance, Hub integrations) in the React portals and gateway BFF.

Master spec: [PLATFORM-SPEC.md §17](../PLATFORM-SPEC.md#17-phase-11--institutional-ui--bff) · Phase 10: [phase-10-services.md](phase-10-services.md)

---

## Goals

1. Gateway proxy and read APIs for governance, tax summaries, surveillance alerts.
2. Investor portal: dividends withholding breakdown, governance vote, self-trade UX.
3. Admin dashboard: governance CRUD, integrations credentials, compliance reports, NAV attestation badge.
4. OpenAPI codegen + Playwright E2E for new flows.

**No new microservices** — extends API Gateway BFF and three React apps.

---

## Track backlog (553–577)

| Track range | Focus |
|-------------|-------|
| 553–557 | Gateway governance proxy; Reporting tax/surveillance read APIs; OpenAPI codegen |
| 558–562 | Investor dividends withholding columns; self-trade error UX |
| 563–567 | Investor governance list + vote pages |
| 568–572 | Admin governance CRUD; integrations credentials page |
| 573–577 | Admin compliance reports; NAV attestation on building detail; BFF aggregates; E2E |

---

## Gateway prerequisites

| Prerequisite | Owner |
|--------------|-------|
| Proxy `/api/v1/governance/**` → `:8100` | API Gateway |
| `GET /v1/reports/tax-summaries` | Reporting |
| `GET /v1/reports/surveillance-alerts` | Reporting |
| Payout/dividend API gross/withholding/net fields | Payment or BFF |
| `GET /v1/bff/investors/{id}/governance-proposals` | API Gateway |
| `GET /v1/bff/admin/reports/compliance` | API Gateway |

---

## Portal routes

See [PLATFORM-SPEC.md §17.3–17.4](../PLATFORM-SPEC.md#173-investor-portal-frontendinvestor-portal-5173) for full route tables (investor + admin).

Tenant portal: no Phase 11 changes.

---

## Related docs

- [investor-portal.md](investor-portal.md)
- [admin-dashboard.md](admin-dashboard.md)
- [api-gateway-bff.md](api-gateway-bff.md)
- [e2e-testing.md](e2e-testing.md)
