# Phase 22 — Building Operations UI

**Status:** Complete (tracks 828–852). **Asset health history**, **admin building health dashboard**, and **investor sustainability health scores**.

Master spec: [PLATFORM-SPEC.md §28](../PLATFORM-SPEC.md#28-phase-22--building-operations-ui)

---

## Goals

1. **Asset health history** — time series of health scores per flat.
2. **Admin building health page** — rollup table across all buildings.
3. **Building detail widget** — health summary on admin building page.
4. **Investor sustainability** — show latest health score alongside ESG profile.

Extends **Reporting** and frontends — no new microservices.

---

## Track backlog (828–852)

| Track range | Focus |
|-------------|-------|
| 828–832 | Asset health history API by flat |
| 833–837 | Admin `/building-health` page |
| 838–842 | Building detail health widget |
| 843–847 | Investor sustainability health score |
| 848–852 | Integration tests; docs |

---

## UI

| Portal | Route | Purpose |
|--------|-------|---------|
| Admin | `/building-health` | Building health rollups table |
| Admin | `/buildings/:id` | Health summary card |
| Investor | `/assets/:flatId/sustainability` | ESG + health score |

---

## Related docs

- [phase-21-services.md](phase-21-services.md) — building health rollups
- [phase-18-services.md](phase-18-services.md) — asset health scores
