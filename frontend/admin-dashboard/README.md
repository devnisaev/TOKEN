# TokenRealty — Admin Dashboard

React + TypeScript admin frontend for property registry overview and KYC compliance review.

## Stack

| Layer | Technology |
|-------|------------|
| Framework | React 19 + TypeScript |
| Build | Vite 6 (`:5174`) |
| Routing | React Router 7 |
| Server state | TanStack Query |
| UI | Tailwind CSS + shadcn-style components |

## Prerequisites

- **API Gateway** `:8080` (proxies all `/api` calls)
- Auth `:8083`, Property Registry `:8081`, Compliance `:8087` (via gateway)

## Run

```bash
cd frontend/admin-dashboard
npm install
npm run dev
```

Open [http://localhost:5174](http://localhost:5174)

Vite proxies `/api` → `http://localhost:8080`.

## Demo login

| Email | Password | Role |
|-------|----------|------|
| `admin@tokenrealty.com` | `admin123` | ADMIN |

Compliance officers and property managers can also sign in (`COMPLIANCE`, `PROPERTY_MANAGER` roles).

## Pages

| Route | Description | API |
|-------|-------------|-----|
| `/login` | Admin JWT sign-in | `POST /v1/auth/login` |
| `/` | Dashboard summary | `GET /v1/buildings`, `GET /v1/compliance` |
| `/buildings` | Building catalog | `GET /v1/buildings` |
| `/compliance` | KYC review queue | `GET /v1/compliance`, `PATCH /v1/compliance/{id}/verify` |

## Environment

| Variable | Default | Description |
|----------|---------|-------------|
| `VITE_API_BASE_URL` | `/api` | API base (use full gateway URL in production) |

## Build

```bash
npm run build
npm run preview
```
