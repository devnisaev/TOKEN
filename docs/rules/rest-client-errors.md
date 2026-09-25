# Rest Client Errors — TokenRealty

Cursor rule: [`.cursor/rules/rest-client-errors.mdc`](../../.cursor/rules/rest-client-errors.mdc)

## Inter-service clients today

| Client | Service | Calls |
|--------|---------|-------|
| `PropertyRegistryClient` | Token Issuance → Registry | GET flat, GET SPV, PATCH token-info |
| `TokenIssuanceClient` | Marketplace → Issuance | GET compliance check |

## Known gaps

- **No auth** on Issuance → Registry calls (401 when secured)
- **No structured error mapping** on PropertyRegistryClient yet
- **Auth Service** will provide service-account JWT for all internal calls

## Target behavior

1. Configure timeout on every `RestClient.Builder`
2. Map 5xx/timeout → `{service}_unavailable`
3. Map 404 → `ResourceNotFoundException` where appropriate
4. Propagate `X-Trace-Id` header
5. Attach `Authorization: Bearer <service-token>` when Auth Service exists
