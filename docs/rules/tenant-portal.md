# Tenant Portal — TokenRealty Frontend

**Status:** MVP implemented in `frontend/tenant-portal/` (dev URL **http://localhost:5175**).

React tenant app — view lease, pay monthly rent, payment history. All API via **API Gateway** `:8080`.

Related: [investor-portal.md](investor-portal.md) · [admin-dashboard.md](admin-dashboard.md)

---

## Run

```bash
cd frontend/tenant-portal
npm install
npm run dev
```

Demo: `tenant@tokenrealty.com` / `tenant123` (user id `22222222-2222-2222-2222-222222222222`).

---

## Pages & routes

| Route | Page | Backend calls |
|-------|------|---------------|
| `/login` | Tenant sign in | `POST /v1/auth/login` |
| `/` | Lease + pay rent | `GET /v1/leases?tenantId=…`, `GET /v1/rent-payments?leaseId=…`, `POST /v1/rent-payments` |

Session key: `tokenrealty.tenant.auth` (separate from investor/admin portals).

Shared fetch + types: `@tokenrealty/shared-api-client`. Layout via `@tokenrealty/shared-ui` (`AppShell`, `ShellHeader`).

Lease page: multi-lease picker when tenant has several leases, payment success banner, full wallet addresses in history.

E2E: smoke `tenant-login.spec.ts`; full `tenant-rent-flow.spec.ts` — see [e2e-testing.md](e2e-testing.md).

Gateway CORS allows `http://localhost:5175`.

---

## Dev seed

- Auth seeds tenant user with fixed UUID
- Registry seeds demo flat `33333333-…` and SPV `44444444-…`
- Rental seeds active lease linking tenant → flat → SPV

Rent payment triggers Payment payout and Kafka `rent.collected` → dividend pipeline when services are running.
