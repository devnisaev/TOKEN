# Phase 20 — Operations Automation

**Status:** Complete (tracks 778–802). **KPI snapshot history**, **scheduled alert generation**, **maintenance ticket signals**, and **auto health recompute** from ESG projections.

Master spec: [PLATFORM-SPEC.md §26](../PLATFORM-SPEC.md#26-phase-20--operations-automation)

---

## Goals

1. **KPI snapshot records** — daily Reporting snapshots for operator dashboard trending.
2. **Scheduled alert generation** — cron scan for health, occupancy, and insurance alerts.
3. **Maintenance ticket signal** — open ticket count on operator KPI dashboard.
4. **Auto health recompute** — derive asset health scores from latest ESG snapshots.
5. **Admin dashboard widget** — open alerts and maintenance tickets on home page.

Extends **Reporting** and **Rental** (read-only client) — no new microservices.

---

## Track backlog (778–802)

| Track range | Focus |
|-------------|-------|
| 778–782 | `OperatorKpiSnapshotRecord` entity + list/record API |
| 783–787 | Scheduled KPI snapshot + alert generation in Reporting |
| 788–792 | Rental client; open maintenance count on KPI dashboard |
| 793–797 | Auto health recompute from ESG snapshots |
| 798–802 | Admin dashboard widget; integration tests; docs |

---

## UI

| Portal | Route | Purpose |
|--------|-------|---------|
| Admin | `/` | Operations widget — open alerts, maintenance tickets |
| Admin | `/operator-kpis` | KPI snapshot history |

---

## Related docs

- [phase-19-services.md](phase-19-services.md) — operator alerts
- [phase-18-services.md](phase-18-services.md) — operator KPIs
