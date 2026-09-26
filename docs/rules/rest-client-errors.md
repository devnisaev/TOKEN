# Rest Client Errors — TokenRealty

Cursor rule: [`.cursor/rules/rest-client-errors.mdc`](../../.cursor/rules/rest-client-errors.mdc)

Inter-service HTTP adapters: transport in `RestClientOperations`, error mapping in `DownstreamClientErrors`, domain URIs in per-service `*Client` classes.

## Architecture

```text
*ClientConfig                    *Client (per service)
     │                                │
     ▼                                ▼
ServiceRestClientBuilder ──► DownstreamRestClientSupport
 (Bearer, timeout, trace)         │ get / post / patchVoid
                                   ▼
                            RestClientOperations  →  Spring RestClient
                                   │
                                   ▼
                            DownstreamClientErrors
                             5xx/timeout → ValidationException
                             404 + message → ResourceNotFoundException
                             4xx → ValidationException + RFC 7807 detail
```

Package: `tokenrealty-web/src/main/java/com/tokenrealty/web/rest/`.

| Class | Purpose |
|-------|---------|
| `RestHeaders` | `X-Trace-Id`, MDC key `traceId`, `Idempotency-Key` |
| `RestClientOperations` | Transport — GET/POST/PATCH/PUT; header helpers `idempotencyKey()` |
| `DownstreamServices` | `ServiceSpec` constants for human-readable error labels |
| `DownstreamClientErrors` | Exception mapping around `Supplier`/`Runnable` calls |
| `DownstreamRestClientSupport` | Protected helpers — extend in each `*Client` |

`TraceIdFilter` (inbound) and `ServiceRestClientBuilder` (outbound) share the same header/MDC names. The security module inlines string constants to avoid a circular Maven dependency on `tokenrealty-web`.

See also [observability.md](observability.md) for trace flow and [shared-libraries.md](shared-libraries.md) for module layout.

---

## Required: use `RestClientOperations`

All inter-service HTTP from `*Client` classes must go through the shared stack — not raw Spring `RestClient`.

| Layer | What to use | Do not |
|-------|-------------|--------|
| Bean factory (`*ClientConfig`) | `ServiceRestClientBuilder.build(...)` | Copy Bearer/timeout/trace interceptors |
| Domain adapter (`*Client`) | `extends DownstreamRestClientSupport` | `restClient.get().uri(...).retrieve()` |
| Transport | `RestClientOperations` (via base `get`/`post`/…) | Duplicate verb + header boilerplate |
| Errors | `DownstreamClientErrors` (automatic via base) | Per-method try/catch on `RestClientResponseException` |
| Headers | `RestClientOperations.idempotencyKey(...)`, `.header(...)` | Ad-hoc `.header()` on raw spec |

For non-standard URIs (query params built with `UriBuilder`), use the base-class overload that accepts `Function<UriBuilder, URI>`, or call `http()` to get the wrapped `RestClientOperations`.

```java
// BAD — bypasses shared transport and error mapping
return restClient.get().uri("/v1/foo/{id}", id).retrieve().body(Foo.class);

// GOOD — DownstreamRestClientSupport → RestClientOperations → DownstreamClientErrors
return get("/v1/foo/{id}", Foo.class, DownstreamServices.PAYMENT, id);
```

Only `*ClientConfig` classes may hold a raw `RestClient` reference (the `@Bean` from `ServiceRestClientBuilder`).

---

## RestClient bean setup

One `@Bean` per downstream base URL in `*ClientConfig`. Always set an explicit timeout for financial paths.

```java
@Configuration
public class PaymentClientConfig {

    @Bean("paymentRestClient")
    RestClient paymentRestClient(
            @Value("${services.payment.url}") String baseUrl,
            ObjectProvider<ServiceTokenProvider> serviceTokenProvider) {
        return ServiceRestClientBuilder.build(
                baseUrl, Duration.ofSeconds(10), serviceTokenProvider);
    }
}
```

Suggested timeouts (tune per SLA):

| Downstream | Typical timeout |
|------------|-----------------|
| Payment | 10s |
| Compliance | 3–5s |
| Registry / Issuance reads | 5–10s |
| Gateway BFF aggregates | 15s+ (caller-side) |

Default when using single-arg `build()`: 10s (`ServiceRestClientBuilder.DEFAULT_TIMEOUT`).

---

## Domain client pattern

Extend `DownstreamRestClientSupport`. Do not wrap calls in try/catch — the base class delegates to `DownstreamClientErrors`.

```java
@Component
public class PaymentClient extends DownstreamRestClientSupport {

    public PaymentClient(@Qualifier("paymentRestClient") RestClient restClient) {
        super(restClient);
    }

    public InitiatePaymentResponse initiateTokenPurchase(UUID orderId, InitiatePaymentRequest body) {
        return post("/v1/payments", body, InitiatePaymentResponse.class,
                DownstreamServices.PAYMENT,
                RestClientOperations.idempotencyKey("marketplace-order-" + orderId));
    }

    public void releaseEscrow(UUID paymentId) {
        patchVoid("/v1/payments/{id}/release", DownstreamServices.PAYMENT, paymentId);
    }
}
```

### Base class helpers

| Method | Use when |
|--------|----------|
| `get(uri, type, service, uriVars…)` | Standard GET; null body is returned as-is |
| `get(uri, type, service, notFoundMessage, uriVars…)` | GET where null or 404 should throw `ResourceNotFoundException` |
| `getAllowNull(uri, type, service, uriVars…)` | 404/null is valid (e.g. compliance wallet not on file) |
| `post(uri, body, type, service, uriVars…)` | POST without extra headers |
| `post(uri, body, type, service, headers, uriVars…)` | POST with `Idempotency-Key` or custom headers |
| `postVoid(uri, body, service, headers, uriVars…)` | POST with empty response |
| `patchVoid(uri, service, uriVars…)` | PATCH with empty response (e.g. escrow release) |

Access raw transport: `http()` returns the wrapped `RestClientOperations`.

---

## Error mapping

| Condition | Exception | Example message |
|-----------|-----------|-----------------|
| 5xx | `ValidationException` | `Payment service unavailable` |
| Timeout / connection failure (`ResourceAccessException`) | `ValidationException` | `Payment service unavailable` |
| 404 when `notFoundMessage` provided | `ResourceNotFoundException` | Caller-supplied message |
| Other 4xx | `ValidationException` | `Payment service rejected request: {detail}` |

4xx messages prefer RFC 7807 `detail` from the downstream body; fall back to `title` or raw body (truncated).

**Rules:**

- Do not catch `ValidationException` / `ResourceNotFoundException` inside clients — let them propagate to `TokenRealtyExceptionHandler`.
- Do not use broad `catch (Exception)` or silent null returns.
- Retries: idempotent GET only; never auto-retry POST without an idempotency key.

---

## Migration status

| Client | Caller | Status |
|--------|--------|--------|
| `PaymentClient` | Marketplace, Rental, Wallet | Done |
| `ComplianceClient` | Marketplace, Issuance | Done |
| `PropertyRegistryClient` | Issuance, Marketplace, Document, Compliance, Gateway | Done |
| `TokenIssuanceClient` | Marketplace, Gateway | Done |
| `MarketplaceClient` | Issuance, Gateway | Done |
| `IssuanceClient` | Wallet, Blockchain Indexer | Done |
| `WalletClient` | Gateway | Done |
| `RentalClient` | Issuance | Done |

### Migrating a legacy client

1. Add or update `*ClientConfig` to use `ServiceRestClientBuilder.build(baseUrl, timeout, provider)`.
2. Change `*Client` to `extends DownstreamRestClientSupport`.
3. Replace try/catch blocks with `get` / `post` / `patchVoid` + appropriate `DownstreamServices.*` constant.
4. Use `getAllowNull` where 404 is not an error.
5. Pass `RestClientOperations.idempotencyKey(...)` on financial POSTs.
6. Run service tests; add cases in `DownstreamClientErrorsTest` only when mapping logic changes.

Reference implementations: `marketplace-service/.../client/PaymentClient.java`, `ComplianceClient.java`.

---

## Service labels (`DownstreamServices`)

| Constant | Label in errors |
|----------|-----------------|
| `PROPERTY_REGISTRY` | Property Registry |
| `TOKEN_ISSUANCE` | Token Issuance service |
| `MARKETPLACE` | Marketplace service |
| `PAYMENT` | Payment service |
| `COMPLIANCE` | Compliance service |
| `DOCUMENT` | Document service |
| `WALLET` | Wallet service |
| `AUTH` | Auth service |

Add a new constant here when introducing a new downstream — do not hard-code service names in client catch blocks.

---

## Testing

Shared unit tests: `tokenrealty-web/src/test/java/com/tokenrealty/web/rest/DownstreamClientErrorsTest.java`.

Per-service tests should mock the `*Client` interface/class at the service layer (existing pattern in `OrderServiceTest`, buy-flow integration tests). Wire-level RestClient tests are optional — prefer testing error mapping once in the shared module.
