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
| `/orders` | Order list | `GET /v1/orders?buyerId=…` |
| `/orders/:id` | Order status (SSE + fallback poll) | `GET /v1/bff/orders/{id}/status-stream`, `GET /v1/orders/{id}`, `GET /v1/orders/{id}/trade` |
| `/portfolio` | Balances + holdings + sell links | `GET /v1/bff/investors/{id}/portfolio` |
| `/portfolio/sell/:contractId` | Secondary sell order | `POST /v1/orders/sell` |
| `/dividends` | Dividend history | `GET /v1/investors/{id}/dividends` |

Protected routes require JWT (`ProtectedRoute` + `AuthProvider`).

---

## Package layout

```
frontend/investor-portal/src/
├── lib/
│   ├── api.ts           ← fetch wrapper; Bearer token injection
│   ├── auth.tsx         ← AuthProvider, sessionStorage tokens
│   └── wagmi.ts         ← Hardhat + Polygon Amoy chains
├── types/api.ts         ← Re-exports from @tokenrealty/shared-api-client + app-only types
├── pages/               ← Login, Listings, ListingDetail, Orders, OrderStatus, Portfolio, SellTokens, Dividends
├── components/
│   ├── ui/              ← Re-exported from @tokenrealty/shared-ui
│   ├── layout/          ← Header (ConnectWalletButton), AppShell via shared-ui
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
4. Redirect to /orders/{id} — SSE stream from BFF + fallback poll until SETTLED
5. Marketplace → KYC check → escrow (Payment) → Kafka settlement (when enabled)
6. Portfolio refreshes via GET /v1/bff/investors/{id}/portfolio
```

KYC must pass before order match ([investment-limits.md](investment-limits.md)). Order status page uses `subscribeSse` on `/v1/bff/orders/{id}/status-stream` until terminal state (SETTLED / CANCELLED / FAILED).

Secondary sell: `/portfolio` → **Sell** link → `/portfolio/sell/:contractId` → `POST /v1/orders/sell`.

---

## Web3 (wagmi)

- Chains: Hardhat (`31337`, RPC `http://127.0.0.1:8545`), Polygon Amoy
- `ConnectWalletButton` in global header and listing buy card — connect → optional `PATCH /v1/users/me/wallet` + `POST /v1/wallets/link`
- Buy/sell orders use connected address or profile `walletAddress`

---

## Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `VITE_API_BASE_URL` | `/api` | Gateway API prefix |

Gateway CORS allows `http://localhost:5173` (`tokenrealty.gateway.cors.allowed-origins`).

---

## Conventions

- **Never** call service ports directly from the browser — always gateway `:8080/api`
- Shared fetch + types: `@tokenrealty/shared-api-client` (BFF types from `shared-api-types/gateway.ts`); app re-exports in `src/types/api.ts`
- Shared layout/components: `@tokenrealty/shared-ui` (`AppShell`, `ShellHeader`, `OrderStatusTimeline`, `SettlementAuditTrail`, shadcn primitives)
- Errors: parse RFC 7807 `detail` from ProblemDetail responses
- No secrets in frontend env — JWT from login only
- Prefer BFF endpoints over N+1 calls to Registry + Marketplace + Issuance

---

## Pending / future

- [x] SSE order status stream (BFF + `subscribeSse`)
- [x] Secondary sell flow (`/portfolio/sell/:contractId`)
- [x] Connect wallet in global header
- [x] Refresh token rotation before access expiry (`shared-api-client/auth-storage`)
- [x] openapi-typescript codegen (`frontend/shared-api-types/` incl. `gateway.yaml`)
- [x] Dividend history page (`/dividends`)
- [x] E2E full buy/sell flows (`frontend/e2e/tests/full/` — requires `E2E_GATEWAY_URL`)
