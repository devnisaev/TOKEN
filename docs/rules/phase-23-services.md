# Phase 23 — Operator Reporting & Trends

**Status:** Complete (tracks 853–877). **Alert summaries**, **health trend detection**, **operations export**, and **weekly operator digest**.

Master spec: [PLATFORM-SPEC.md §29](../PLATFORM-SPEC.md#29-phase-23--operator-reporting--trends)

---

## Goals

1. **Operator alert summary** — counts by alert type and severity.
2. **Health trend detection** — flats with declining composite scores.
3. **Operations export** — aggregated operator KPI + alert + trend snapshot.
4. **Weekly digest** — scheduled notification with operations summary.
5. **Admin UI** — `/reports/operations` dashboard.

Extends **Reporting** and **Notification** — no new microservices.

---

## Track backlog (853–877)

| Track range | Focus |
|-------------|-------|
| 853–857 | Operator alert summary API |
| 858–862 | Health trend detection service + API |
| 863–867 | Operations export endpoint |
| 868–872 | Weekly operator digest scheduler |
| 873–877 | Admin operations reports page; tests; docs |

---

## UI

| Portal | Route | Purpose |
|--------|-------|---------|
| Admin | `/reports/operations` | Alert summary, declining health, KPI snapshot |

---

## Related docs

- [phase-22-services.md](phase-22-services.md) — building health UI
- [phase-20-services.md](phase-20-services.md) — KPI snapshots
