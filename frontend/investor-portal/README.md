# TokenRealty — Investor Portal

React + TypeScript investor frontend for browsing listings, placing buy orders, and viewing portfolio balances.

## Stack

| Layer | Technology |
|-------|------------|
| Framework | React 19 + TypeScript |
| Build | Vite 6 |
| Routing | React Router 7 |
| Server state | TanStack Query |
| Web3 | wagmi + viem (MetaMask / Hardhat) |
| UI | Tailwind CSS + shadcn-style components |

## Prerequisites

Backend services running locally:

- **API Gateway** `:8080` (proxies all `/api` calls)
- Auth `:8083`, Marketplace `:8084`, Payment `:8085`, Wallet `:8090` (via gateway)

```bash
# From repo root — minimal stack for portal dev
docker compose up -d postgres
# Start gateway + auth + marketplace + payment + wallet (+ registry/issuance for listings)
```

## Run

```bash
cd frontend/investor-portal
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173)

Vite proxies `/api` → `http://localhost:8080` (see `vite.config.ts`).

## Demo login

| Email | Password |
|-------|----------|
| `investor@tokenrealty.com` | `investor123` |

Hardhat demo wallet (account #1): `0x70997970c51812dc3a010c724d1afe6fc599aa84`

## Pages

| Route | Description |
|-------|-------------|
| `/login` | JWT sign-in |
| `/` | Active listings grid |
| `/listings/:id` | BFF detail + buy order form |
| `/portfolio` | Wallet aggregate balance (USDC + tokens) |

## Environment

| Variable | Default | Description |
|----------|---------|-------------|
| `VITE_API_BASE_URL` | `/api` | API base (use full gateway URL in production) |

## Build

```bash
npm run build
npm run preview
```
