# E2E Testing — TokenRealty Frontends

Playwright workspace: [`frontend/e2e/`](../../frontend/e2e/)

## Smoke tests (CI)

No backend required — uses Vite `preview` builds of all three portals.

```bash
cd frontend/shared-ui && npm ci
for app in investor-portal admin-dashboard tenant-portal; do
  cd ../$app && npm ci && npm run build && cd ..
done
cd frontend/e2e && npm ci && npx playwright install --with-deps && npm run test:smoke
```

Covers: login page render, protected-route redirect to `/login` (6 tests across three portals).

CI job: `.github/workflows/ci.yml` → `frontend-e2e-smoke`.

## Full flow tests (local)

Requires a live gateway and seeded demo data:

```bash
./scripts/demo-start.sh          # Postgres + Kafka + Jaeger
# Start minimum stack: auth, registry, payment, marketplace, gateway (see script output)
./scripts/seed-demo.sh

export E2E_GATEWAY_URL=http://localhost:8080
cd frontend/e2e && npm run test:full
```

Full specs skip automatically when `E2E_GATEWAY_URL` is unset.

| Spec | Flow |
|------|------|
| `tests/full/investor-login-flow.spec.ts` | Login → listings nav |
| `tests/full/investor-buy-flow.spec.ts` | Login → listing → place buy order → order status page |
| `tests/full/investor-secondary-sell-flow.spec.ts` | Login → portfolio → sell form (skips if no holdings) |
| `tests/full/admin-kyc-flow.spec.ts` | Admin login → compliance page |
| `tests/full/tenant-rent-flow.spec.ts` | Tenant login → lease view |

CI: `frontend-e2e-full` runs on `workflow_dispatch` when repo secret `E2E_GATEWAY_URL` is set.

## Demo infrastructure

```bash
./scripts/demo-start.sh              # Postgres + Kafka + Jaeger + startup checklist
./scripts/demo-start.sh --infra-only  # Postgres + Kafka only (no Jaeger)
./scripts/demo-start.sh --seed       # Also runs seed-demo.sh if gateway :8080 is up
```

Jaeger UI: http://localhost:16686 (OTLP HTTP `:4318`).

See [observability.md](observability.md) for `OTEL_ENABLED` on services.

## Adding tests

| Folder | When |
|--------|------|
| `tests/smoke/` | UI-only; runs in CI on every push |
| `tests/full/` | Live API; gated on `E2E_GATEWAY_URL` |

Use project name filter for one portal: `npx playwright test --project=investor-portal`.

Pattern for full tests:

```typescript
const gatewayUrl = process.env.E2E_GATEWAY_URL;
test.describe('My flow', () => {
  test.skip(!gatewayUrl, 'Set E2E_GATEWAY_URL to run against a live gateway');
  // ...
});
```
