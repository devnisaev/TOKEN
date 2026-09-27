# Phase 19 — Operations Alerting & Automation

**Status:** Complete (tracks 753–777). **Operator alert records**, **scheduled insurance expiry sweep**, and **alert generation** from health, ESG, and insurance signals.

Master spec: [PLATFORM-SPEC.md §25](../PLATFORM-SPEC.md#25-phase-19--operations-alerting--automation)

---

## Goals

1. **Operator alert records** — Reporting projections for health-at-risk, low occupancy, and insurance expiry.
2. **Alert generation API** — scan local projections + Registry insurance expiry feed.
3. **Insurance expiry scheduler** — daily sweep marks overdue policies `EXPIRED` in Registry.
4. **Admin UI** — `/operator-alerts` with acknowledge action and open-alert count on KPI dashboard.

Extends **Property Registry** and **Reporting** — no new microservices.

---

## Track backlog (753–777)

| Track range | Focus |
|-------------|-------|
| 753–757 | `OperatorAlertRecord` entity + list/generate/ack API in Reporting |
| 758–762 | Property Registry client in Reporting for insurance expiry ingest |
| 763–767 | Scheduled insurance expiry sweep in Registry |
| 768–772 | Admin operator alerts page; KPI open-alert widget |
| 773–777 | Integration tests; docs |

---

## UI

| Portal | Route | Purpose |
|--------|-------|---------|
| Admin | `/operator-alerts` | Open operator alerts; acknowledge |
| Admin | `/operator-kpis` | KPI cards + open alert count |

---

## Related docs

- [phase-18-services.md](phase-18-services.md) — operator KPIs, asset health scores
- [phase-17-services.md](phase-17-services.md) — insurance registry
