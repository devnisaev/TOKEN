# Investor Portal — TokenRealty Frontend

**Status:** MVP implemented in `frontend/investor-portal/` (dev URL **http://localhost:5173**).

React investor app — browse listings, place buy orders, view portfolio. All API traffic goes through **API Gateway** `:8080`.

Related: [api-gateway-bff.md](api-gateway-bff.md) · [hardhat-demo.md](../hardhat-demo.md) · [investment-limits.md](investment-limits.md)

---

## Stack

| Layer | Technology |
|-------|------------|
| Framework | React 19 + TypeScript |
| Build | Vite 6 (`:5173`) |
| Routing | React Router 7 |
| Server state | TanStack Query |
| Web3 | wagmi + viem (MetaMask, Hardhat chain `31337`) |
| UI | Tailwind CSS + shadcn-style components |
| Auth | JWT via Auth Service (sessionStorage in dev) |

---

## Run

```bash
# Prerequisites: API Gateway :8080 + Auth, Marketplace, Payment, Wallet (min)
cd frontend/investor-portal
npm install
npm run dev
```

Vite proxies `/api` → `http://localhost:8080` (see `vite.config.ts`). Production: set `VITE_API_BASE_URL` to the gateway URL.

---

## Pages & routes

| Route | Page | Backend calls |
|-------|------|---------------|
| `/login` | Sign in | `POST /v1/auth/login` |
| `/` | Listings grid | `GET /v1/listings?status=ACTIVE` |
| `/listings/:id` | Detail + buy | `GET /v1/bff/listings/{id}`, `POST /v1/orders` |
| `/portfolio` | Balances + holdings | `GET /v1/wallets/{investorId}/balance` |

Protected routes require JWT (`ProtectedRoute` + `AuthProvider`).

---

## Package layout

```
frontend/investor-portal/src/
├── lib/
│   ├── api.ts           ← fetch wrapper; Bearer token injection
│   ├── auth.tsx         ← AuthProvider, sessionStorage tokens
│   └── wagmi.ts         ← Hardhat + Polygon Amoy chains
├── types/api.ts         ← TypeScript mirrors of backend DTOs
├── pages/               ← Login, Listings, ListingDetail, Portfolio
├── components/
│   ├── ui/              ← Button, Card, Input, Label
│   ├── layout/          ← Header, AppLayout
│   └── ConnectWalletButton.tsx
└── App.tsx              ← React Router routes
```

---

## Auth flow

```text
Login → POST /api/v1/auth/login
     → store accessToken + refreshToken in sessionStorage
     → GET /api/v1/users/me on load
All requests → Authorization: Bearer {accessToken}
```

Demo credentials (Auth `DevDataInitializer`):

| Email | Password | Role |
|-------|----------|------|
| `investor@tokenrealty.com` | `investor123` | INVESTOR |
| `admin@tokenrealty.com` | `admin123` | ADMIN |

Hardhat demo investor: `11111111-1111-1111-1111-111111111111`, wallet `0x70997970c51812dc3a010c724d1afe6fc599aa84`.

---

## Buy flow (UI → backend)

```text
1. User opens /listings/:id  → BFF aggregate (flat + token + listing)
2. Connect MetaMask (optional) or use profile walletAddress
3. POST /v1/orders { listingId, buyerId, buyerWallet, tokenAmount }
4. Marketplace → KYC check → escrow (Payment) → Kafka settlement (when enabled)
5. Portfolio refreshes via GET /v1/wallets/{id}/balance
```

KYC must pass before order match ([investment-limits.md](investment-limits.md)). UI shows order ID on success; payment/transfer progress is async via Kafka.

---

## Web3 (wagmi)

- Chains: Hardhat (`31337`, RPC `http://127.0.0.1:8545`), Polygon Amoy
- `ConnectWalletButton` — connect → optional `PATCH /v1/users/me/wallet` + `POST /v1/wallets/link`
- Buy order uses connected address or profile `walletAddress`

---

## Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `VITE_API_BASE_URL` | `/api` | Gateway API prefix |

Gateway CORS allows `http://localhost:5173` (`tokenrealty.gateway.cors.allowed-origins`).

---

## Conventions

- **Never** call service ports directly from the browser — always gateway `:8080/api`
- Types in `src/types/api.ts` — update when backend DTOs change; openapi-typescript planned
- Errors: parse RFC 7807 `detail` from ProblemDetail responses
- No secrets in frontend env — JWT from login only
- Prefer BFF endpoints over N+1 calls to Registry + Marketplace + Issuance

---

## Pending / future

- [ ] Admin dashboard (`frontend/admin-dashboard/`)
- [ ] Tenant portal (rent pay)
- [ ] Order status polling / WebSocket after buy
- [ ] Refresh token rotation before access expiry
- [ ] openapi-typescript codegen from springdoc
- [ ] Dividend history page
- [ ] E2E tests (Playwright against docker-compose)
