# Phase 18 — Operator Analytics

**Status:** Complete (tracks 728–752). **Operator KPI dashboards**, **asset health composite scores**, and **insurance expiry alerts** for portfolio operators.

Master spec: [PLATFORM-SPEC.md §24](../PLATFORM-SPEC.md#24-phase-18--operator-analytics)

---

## Goals

1. **Insurance expiry alerts** — Registry query for ACTIVE policies expiring within N days.
2. **Asset health scores** — Reporting composite score from ESG, occupancy, and insurance coverage factors.
3. **Operator KPI dashboard** — aggregated occupancy, carbon, health score, and at-risk asset counts.
4. **Admin UI** — `/operator-kpis` dashboard with health scores and insurance expiry table.

Extends **Property Registry** and **Reporting** — no new microservices.

---

## Track backlog (728–752)

| Track range | Focus |
|-------------|-------|
| 728–732 | Insurance expiry query API in Property Registry |
| 733–737 | Asset health score entity + record/list API in Reporting |
| 738–742 | Operator KPI aggregation service |
| 743–747 | Admin operator KPI dashboard UI |
| 748–752 | Integration tests; docs; gateway routes verified |

---

## UI

| Portal | Route | Purpose |
|--------|-------|---------|
| Admin | `/operator-kpis` | KPI cards, asset health table, insurance expiry alerts |
| Admin | `/esg` | ESG profiles (Phase 17; linked from operator nav) |

---

## Related docs

- [phase-17-services.md](phase-17-services.md) — ESG, insurance, IoT feeds
- [phase-12-services.md](phase-12-services.md) — AssetUnit occupancy
