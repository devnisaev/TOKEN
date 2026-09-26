# Rest Client Errors — TokenRealty

Cursor rule: [`.cursor/rules/rest-client-errors.mdc`](../../.cursor/rules/rest-client-errors.mdc)

## RestClient bean setup

Use shared `ServiceRestClientBuilder` from `tokenrealty-security` — do not copy the Bearer interceptor boilerplate.

```java
import com.tokenrealty.security.client.ServiceRestClientBuilder;

@Configuration
public class PaymentClientConfig {

    @Bean("paymentRestClient")
    RestClient paymentRestClient(
            @Value("${services.payment.url}") String baseUrl,
            ObjectProvider<ServiceTokenProvider> serviceTokenProvider) {
        return ServiceRestClientBuilder.build(baseUrl, serviceTokenProvider);
    }
}
```

Domain clients (`PaymentClient`, `TokenIssuanceClient`, …) stay per-service. See [shared-libraries.md](shared-libraries.md).

## Inter-service clients today

| Client | Service | Calls | Auth |
|--------|---------|-------|------|
| `PropertyRegistryClient` | Token Issuance → Registry | GET flat, GET SPV, PATCH token-info | Service token |
| `ComplianceClient` | Marketplace, Issuance → Compliance | KYC check (`isWhitelisted`, `investorId`) | Service token |
| `TokenIssuanceClient` | Marketplace → Issuance | Contract by flat, holder balance by wallet, transfer | Service token |
| `PaymentClient` | Marketplace, Rental → Payment | Initiate escrow, release escrow, payouts | Service token |
| `MarketplaceClient` | Token Issuance → Marketplace | GET trade by orderId (`listingType`, `sellerWallet`) | Service token |

## Service name prefixes

| Downstream | Prefix |
|------------|--------|
| Property Registry | `property_registry_` |
| Token Issuance | `token_issuance_` |
| Marketplace | `marketplace_` |
| Payment | `payment_` |
| Compliance | `compliance_` |
| Auth | `auth_` |

## Target behavior

1. Configure timeout on every `RestClient.Builder`
2. Map 5xx/timeout → `{service}_unavailable` → `ValidationException` (from `tokenrealty-web`) or domain error
3. Map 404 → `ResourceNotFoundException` where appropriate
4. Propagate `X-Trace-Id` header
5. Attach `Authorization: Bearer` via `ServiceTokenProvider` (`tokenrealty-security`)

## Pending

- Structured error mapping on `PropertyRegistryClient` and `TokenIssuanceClient`
- Structured error mapping on `PaymentClient`
