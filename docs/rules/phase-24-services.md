# Phase 24 — Lease Coverage & Vacancy Alerts

**Status:** Complete (tracks 878–902). **Active lease APIs**, **lease coverage summary**, **vacancy risk alerts**, and **operations export integration**.

Master spec: [PLATFORM-SPEC.md §30](../PLATFORM-SPEC.md#30-phase-24--lease-coverage--vacancy-alerts)

---

## Goals

1. **Active/expiring lease APIs** in Rental Service for operator consumption.
2. **Lease coverage summary** in Reporting via Rental client.
3. **Vacancy risk alerts** when low occupancy ESG and no active lease.
4. **Operations export** extended with lease coverage stats.
5. **Admin UI** lease section on operations reports page.

Extends **Rental Service** and **Reporting** — no new microservices.

---

## Track backlog (878–902)

| Track range | Focus |
|-------------|-------|
| 878–882 | Active/expiring lease query APIs in Rental |
| 883–887 | Rental client + lease coverage service in Reporting |
| 888–892 | Vacancy risk alert generation |
| 893–897 | Operations export lease section |
| 898–902 | Admin UI; integration tests; docs |

---

## UI

| Portal | Route | Purpose |
|--------|-------|---------|
| Admin | `/reports/operations` | Lease coverage + expiring leases |

---

## Related docs

- [phase-23-services.md](phase-23-services.md) — operations export
- [phase-19-services.md](phase-19-services.md) — operator alerts
