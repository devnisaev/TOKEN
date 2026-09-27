# Phase 12 — Universal Asset Tokenization

**Status:** Planned (tracks 578–602). Generalizes the platform from **Building → Flat** to **any tokenizable real-estate asset** — private houses, land, gyms, pools, service stations, warehouses, hospitality.

Master spec: [PLATFORM-SPEC.md §18](../PLATFORM-SPEC.md#18-phase-12--universal-asset-tokenization) · Diagram: [09-phase-12-asset-generalization.puml](../diagrams/09-phase-12-asset-generalization.puml)

---

## Goals

1. Introduce **`AssetUnit`** concept (evolve `Flat` or alias) — one ERC-1400 contract per unit.
2. Expand **`PropertyCategory`** for operating and leisure assets.
3. Model **income archetypes**: rent, operator revenue share, membership, land appreciation.
4. Operator KPI ingestion for commercial assets (gym members, station throughput, pool utilization).
5. **Liquidity tier** per asset — drives exchange trading rules in Phases 13–15.

**Extend existing services** — Property Registry, Rental, Valuation, Document, Marketplace, Search. No new `:8100+` service unless operator-ingest volume warrants a dedicated adapter in Integration Hub.

---

## Asset archetypes

| Archetype | Examples | Token unit | Typical income | Key documents |
|-----------|----------|------------|----------------|---------------|
| Residential whole | House, villa | 1 property = 1 contract (fractional shares) | Long-term / short-stay rent | Title deed, survey, zoning |
| Land | Plot, agricultural, development | Fractional land rights / SPV equity | Appreciation; ground lease | Cadastral ref, land-use certificate |
| Operating commercial | Gym, pool, car wash, service station | Revenue-share or NNN lease tokens | Operator P&L, franchise fees | Operator agreement, licenses, environmental |
| Hospitality / leisure | Hotel wing, marina berth | Revenue pool | ADR × occupancy | Management agreement |
| Industrial | Warehouse, logistics hub | NNN lease-backed | Triple-net rent | Tenant lease, capex plan |

Standalone assets use **Building as 1:1 wrapper** (same API, simpler admin UX).

---

## Registry metadata (new / extended)

| Field | Entity | Purpose |
|-------|--------|---------|
| `propertyCategory` | `Building` / `AssetUnit` | Add `GYM`, `SWIMMING_POOL`, `SERVICE_STATION`, `HOSPITALITY`, … |
| `operatingModel` | `AssetUnit` | `PURE_RENT`, `OPERATOR_REVENUE_SHARE`, `MEMBERSHIP`, `DEVELOPMENT` |
| `liquidityTier` | `AssetUnit` | `TIER_1` (daily CLOB), `TIER_2` (weekly), `TIER_3` (RFQ only) |
| `licenseTypes[]` | `AssetUnit` | Health permit, fuel storage, pool safety cert |
| `developmentStage` | Land units | `RAW`, `PERMITTED`, `UNDER_CONSTRUCTION`, `STABILIZED` |
| `environmentalRiskTier` | `AssetUnit` | Gas stations, industrial — compliance + insurance |
| `occupancyOrUtilization` | Projections / Valuation | Hotel %, gym member count, station throughput |

Document types (via `PropertyDocument`): `OPERATING_LICENSE`, `ENVIRONMENTAL_AUDIT`, `OPERATOR_AGREEMENT`, `FRANCHISE_AGREEMENT`, `FUEL_STORAGE_PERMIT`.

---

## Service changes

| Service | Deliverable |
|---------|-------------|
| Property Registry | `AssetUnit` API; expanded categories; cadastral on land |
| Rental | Operator revenue-share schedules; non-lease income types |
| Valuation | KPI-linked NAV for operating assets; land comparables |
| Document | New document types in data room |
| Integration Hub | Optional operator P&L webhook adapter |
| Marketplace | Category filters; liquidity tier on listings |
| Search | Index new categories + operating model |
| Corporate Actions | Dividend from operator revenue (not just rent) |

---

## Track backlog (578–602)

| Track range | Focus |
|-------------|-------|
| 578–582 | `PropertyCategory` enum expansion; `operatingModel`, `liquidityTier` on registry |
| 583–587 | AssetUnit alias/API; standalone house + land registration flows |
| 588–592 | Document types; compliance review for operating licenses |
| 593–597 | Operator revenue ingestion (Hub or Rental extension); valuation KPI fields |
| 598–602 | Marketplace/Search filters; integration tests; admin UI asset registration (Phase 11 overlap) |

---

## Exchange readiness

Each `AssetUnit` carries **`liquidityTier`** consumed by Phase 13 CLOB and Phase 14 AMM:

| Tier | Secondary trading |
|------|-------------------|
| `TIER_1` | Full CLOB + optional AMM (apartments, stabilized commercial) |
| `TIER_2` | CLOB with wider NAV band; weekly auction fallback |
| `TIER_3` | RFQ / OTC only (raw land, pre-stabilization) |

---

## Related docs

- [rwa-legal-compliance skill](../../.cursor/skills/rwa-legal-compliance/SKILL.md)
- [phase-13-services.md](phase-13-services.md)
