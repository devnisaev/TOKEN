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
# Start minimum stack: auth, registry, payment, marketplace, rental, notification, gateway
./scripts/demo-services.sh
./scripts/wait-for-services.sh
./scripts/seed-demo.sh

export E2E_GATEWAY_URL=http://localhost:8080
./scripts/e2e-run.sh
```

Or manually:

```bash
./scripts/wait-for-services.sh              # default: gateway + auth + registry + marketplace + payment + rental + notification
SERVICES="8080" ./scripts/wait-for-services.sh --timeout 120   # gateway only
cd frontend/e2e && npm run test:full
```

`e2e-run.sh` waits for `:8080`, runs `npm ci`, installs Playwright Chromium, then `npm run test:full`. Pass `--no-wait` to skip the health check.

Full specs skip automatically when `E2E_GATEWAY_URL` is unset.

| Spec | Flow |
|------|------|
| `tests/full/investor-login-flow.spec.ts` | Login → listings nav |
| `tests/full/investor-buy-flow.spec.ts` | Login → listing → place buy order → order status page |
| `tests/full/investor-secondary-sell-flow.spec.ts` | Login → portfolio → sell form (skips if no holdings) |
| `tests/full/admin-kyc-flow.spec.ts` | Admin login → compliance page |
| `tests/full/admin-document-review-flow.spec.ts` | Admin login → document reviews → approve (or empty queue) |
| `tests/full/tenant-rent-flow.spec.ts` | Tenant login → lease view → pay rent (or already-paid) |

CI: `frontend-e2e-full` runs on `workflow_dispatch` when repo secret `E2E_GATEWAY_URL` is set.

## Demo infrastructure

```bash
./scripts/demo-start.sh              # Postgres + Kafka + Jaeger + startup checklist
./scripts/demo-start.sh --infra-only  # Postgres + Kafka only (no Jaeger)
./scripts/demo-start.sh --seed       # Also runs seed-demo.sh if gateway :8080 is up
./scripts/demo-services.sh           # Start full demo backend (auth → document → wallet → indexer → gateway)
./scripts/demo-services.sh --stop    # Stop background Spring Boot processes
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
