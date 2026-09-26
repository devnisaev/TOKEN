# Admin Dashboard — TokenRealty Frontend

**Status:** MVP implemented in `frontend/admin-dashboard/` (dev URL **http://localhost:5174**).

React admin app — buildings overview, KYC compliance review. All API traffic goes through **API Gateway** `:8080`.

Related: [investor-portal.md](investor-portal.md) · [api-gateway-bff.md](api-gateway-bff.md) · [investment-limits.md](investment-limits.md)

---

## Stack

| Layer | Technology |
|-------|------------|
| Framework | React 19 + TypeScript |
| Build | Vite 6 (`:5174`) |
| Routing | React Router 7 |
| Server state | TanStack Query |
| UI | Tailwind CSS + `@tokenrealty/shared-ui` (AppShell, shadcn primitives) |
| Auth | JWT via Auth Service (separate sessionStorage key) |

---

## Run

```bash
cd frontend/admin-dashboard
npm install
npm run dev
```

Vite proxies `/api` → `http://localhost:8080`. Production: set `VITE_API_BASE_URL` to the gateway URL.

---

## Pages & routes

| Route | Page | Backend calls |
|-------|------|---------------|
| `/login` | Admin sign in | `POST /v1/auth/login` |
| `/` | Dashboard | `GET /v1/buildings`, `GET /v1/compliance` |
| `/buildings` | Building list | `GET /v1/buildings` |
| `/compliance` | KYC queue + status tabs + approve | `GET /v1/compliance?status=`, `PATCH /v1/compliance/{id}/verify` |
| `/document-reviews` | Data room doc queue + building drill-down | `GET /v1/compliance/document-reviews/pending`, `PATCH …/verify` |
| `/buildings/new` | Register building | `POST /v1/buildings` |
| `/buildings/:id` | Building detail + flats | `GET /v1/bff/buildings/{id}` |
| `/buildings/:id/edit` | Edit building | `GET /v1/buildings/{id}`, `PUT /v1/buildings/{id}` |
| `/buildings/:id/flats/new` | Add flat | `POST /v1/buildings/{id}/flats` |
| `/buildings/:id/flats/:flatId/edit` | Edit flat | `GET /v1/flats/{id}`, `PUT /v1/flats/{id}` |
| `/orders` | Order monitoring + status filter tabs | `GET /v1/orders?status=&size=50` |
| `/orders/:id` | Order detail + manual settle | `GET /v1/orders/{id}`, `GET /v1/orders/{id}/trade`, `PATCH /v1/orders/{id}/settle` |

Protected routes require JWT and role `ADMIN`, `COMPLIANCE`, or `PROPERTY_MANAGER` (`AdminRoute`).

KYC verify requires `ADMIN` or `COMPLIANCE` on the backend — property managers see buildings only.

---

## Auth

- Session key: `tokenrealty.admin.auth` (separate from investor portal)
- Demo: `admin@tokenrealty.com` / `admin123`

Gateway CORS allows `http://localhost:5174` (`tokenrealty.gateway.cors.allowed-origins`).

---

## Conventions

- **Never** call service ports directly from the browser — always gateway `:8080/api`
- Shared fetch + types: `@tokenrealty/shared-api-client` (`frontend/shared-api-client/`)
- App-specific types in `src/types/api.ts` — extend openapi types with required fields
- Errors: parse RFC 7807 `detail` from ProblemDetail responses
- No secrets in frontend env — JWT from login only

---

## Pending / future

- [x] Flat create/edit within building detail
- [x] Order monitoring with status filters (All / Matched / Settled / Cancelled)
- [x] Order detail page with manual settle form (`SettleTradeRequest`)
- [x] Compliance status filter tabs
- [x] Document review drill-down (building link, metadata)
- [x] Settlement timeline on order detail (`OrderStatusTimeline` from `@tokenrealty/shared-ui`)
- [x] Settlement audit trail on order detail (`SettlementAuditTrail` — order/trade timestamps + payment/transfer IDs)
- [x] E2E smoke + full KYC flow (`frontend/e2e/tests/full/admin-kyc-flow.spec.ts`)
