# Rest Client Errors — TokenRealty

Cursor rule: [`.cursor/rules/rest-client-errors.mdc`](../../.cursor/rules/rest-client-errors.mdc)

## Inter-service clients today

| Client | Service | Calls | Auth |
|--------|---------|-------|------|
| `PropertyRegistryClient` | Token Issuance → Registry | GET flat, GET SPV, PATCH token-info | Service token |
| `TokenIssuanceClient` | Marketplace → Issuance | GET compliance check | Service token |
| `PaymentClient` | Marketplace → Payment | (planned) POST initiate payment | Service token |

## Service name prefixes

| Downstream | Prefix |
|------------|--------|
| Property Registry | `property_registry_` |
| Token Issuance | `token_issuance_` |
| Marketplace | `marketplace_` |
| Payment | `payment_` |
| Auth | `auth_` |

## Target behavior

1. Configure timeout on every `RestClient.Builder`
2. Map 5xx/timeout → `{service}_unavailable` → `ValidationException` or domain error
3. Map 404 → `ResourceNotFoundException` where appropriate
4. Propagate `X-Trace-Id` header
5. Attach `Authorization: Bearer` via `ServiceTokenProvider` (`tokenrealty-security`)

## Pending

- Structured error mapping on `PropertyRegistryClient` and `TokenIssuanceClient`
- `PaymentClient` when Marketplace ↔ Payment integration lands
