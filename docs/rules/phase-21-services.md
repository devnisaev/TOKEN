# Phase 21 — Portfolio Health & Alert Notifications

**Status:** Complete (tracks 803–827). **Critical alert notifications**, **building health rollups**, **acknowledged alert history**, and **investor portfolio health view**.

Master spec: [PLATFORM-SPEC.md §27](../PLATFORM-SPEC.md#27-phase-21--portfolio-health--alert-notifications)

---

## Goals

1. **Critical alert notifications** — email/log via Notification Service when CRITICAL operator alerts are created.
2. **Acknowledged alert history** — paginated list of resolved operator alerts.
3. **Building health rollups** — aggregate asset health scores per building.
4. **Portfolio health API** — latest health scores for a set of flat IDs.
5. **Investor UI** — `/portfolio/health` page linking holdings to health scores.

Extends **Reporting** and **Notification** — no new microservices.

---

## Track backlog (803–827)

| Track range | Focus |
|-------------|-------|
| 803–807 | Notification client; CRITICAL alert dispatch on generate |
| 808–812 | Acknowledged alerts history API |
| 813–817 | Building health rollup + portfolio health by flat IDs |
| 818–822 | Investor portfolio health page |
| 823–827 | Admin acknowledged alerts tab; integration tests; docs |

---

## UI

| Portal | Route | Purpose |
|--------|-------|---------|
| Investor | `/portfolio/health` | Holdings with latest health scores |
| Admin | `/operator-alerts` | Open + acknowledged alert tabs |

---

## Related docs

- [phase-20-services.md](phase-20-services.md) — KPI snapshots, automation
- [phase-19-services.md](phase-19-services.md) — operator alerts
