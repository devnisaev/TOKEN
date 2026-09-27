# Phase 25 — Maintenance Backlog & Rent Collection Ops

**Status:** Complete (tracks 903–927). **Maintenance backlog summary**, **lease expiry alerts**, **rent collection summary**, and **operations export integration**.

Master spec: [PLATFORM-SPEC.md §31](../PLATFORM-SPEC.md#31-phase-25--maintenance-backlog--rent-collection-ops)

---

## Goals

1. **Maintenance backlog summary** — flats with multiple open maintenance tickets.
2. **Lease expiry operator alerts** from Rental expiring lease feed.
3. **Rent collection summary** from Reporting rent-collected projections.
4. **Operations export** extended with maintenance and rent sections.
5. **Admin UI** maintenance/rent cards on operations reports page.
6. **Tenant portal** lease expiry banner when end date within 30 days.

Extends **Reporting Service** and frontends — no new microservices.

---

## Track backlog (903–927)

| Track range | Focus |
|-------------|-------|
| 903–907 | Rental client open-ticket listing; maintenance backlog service |
| 908–912 | Lease expiry + maintenance backlog alert generation |
| 913–917 | Rent collection summary service |
| 918–922 | Operations export extension |
| 923–927 | Admin UI; tenant lease banner; integration tests; docs |

---

## UI

| Portal | Route | Purpose |
|--------|-------|---------|
| Admin | `/reports/operations` | Maintenance backlog + rent collection cards |
| Tenant | `/` (lease) | Expiring lease warning banner |

---

## Related docs

- [phase-24-services.md](phase-24-services.md) — lease coverage
- [phase-19-services.md](phase-19-services.md) — operator alerts
