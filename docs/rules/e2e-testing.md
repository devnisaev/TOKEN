# E2E Testing — TokenRealty Frontends

Playwright workspace: [`frontend/e2e/`](../../frontend/e2e/)

## Smoke tests (CI)

No backend required — uses Vite `preview` builds of all three portals.

```bash
cd frontend/investor-portal && npm ci && npm run build
cd ../admin-dashboard && npm ci && npm run build
cd ../tenant-portal && npm ci && npm run build
cd ../e2e && npm ci && npx playwright install --with-deps && npm run test:smoke
```

Covers: login page render, protected-route redirect to `/login`.

## Full flow tests (local)

Requires platform running (gateway `:8080` + Auth + downstream services):

```bash
export E2E_GATEWAY_URL=http://localhost:8080
cd frontend/e2e && npm run test:full
```

## Docker / compose

Start Postgres and services per [AGENTS.md](../../AGENTS.md), then run full tests. A dedicated `e2e` compose profile is not required for smoke CI.

## Adding tests

| Folder | When |
|--------|------|
| `tests/smoke/` | UI-only; runs in CI |
| `tests/full/` | Live API; gated on `E2E_GATEWAY_URL` |

Use project name filter for one portal: `npx playwright test --project=investor-portal`.
