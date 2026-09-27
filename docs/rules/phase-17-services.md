# Phase 17 — ESG & Asset Operations

**Status:** Complete (tracks 703–727). **ESG metrics**, **IoT utilization feeds**, **insurance metadata**, and **operator KPI dashboards** for universal assets.

Master spec: [PLATFORM-SPEC.md §23](../PLATFORM-SPEC.md#23-phase-17--esg--asset-operations)

---

## Goals

1. **ESG profile** per building/flat — carbon score, energy rating, environmental risk tier.
2. **IoT utilization feeds** — Integration Hub ingests occupancy/throughput readings → Registry update.
3. **Insurance policy registry** — coverage metadata linked to tokenized assets.
4. **Reporting projections** — ESG snapshot records for admin compliance dashboards.
5. **UI** — admin ESG overview; investor asset sustainability view.

Extends **Property Registry**, **Integration Hub**, **Reporting** — no new microservices.

---

## Track backlog (703–727)

| Track range | Focus |
|-------------|-------|
| 703–707 | ESG profile entity + CRUD API in Property Registry |
| 708–712 | IoT reading ingest in Integration Hub; Registry occupancy update |
| 713–717 | Insurance policy entity + API |
| 718–722 | Reporting ESG snapshot projections |
| 723–727 | Admin ESG dashboard; investor sustainability page; E2E tests |

---

## UI

| Portal | Route | Purpose |
|--------|-------|---------|
| Admin | `/esg` | ESG scores, IoT feed status, insurance expiry |
| Investor | `/assets/:id/sustainability` | Asset ESG + utilization summary |

---

## Related docs

- [phase-12-services.md](phase-12-services.md) — AssetUnit, occupancyOrUtilization
- [phase-10-services.md](phase-10-services.md) — IoT/ESG deferred items
