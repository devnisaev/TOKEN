# tokenrealty-security

Shared JWT validation and service-account client for TokenRealty microservices.

## Usage

Add dependency:

```xml
<dependency>
    <groupId>com.tokenrealty</groupId>
    <artifactId>tokenrealty-security</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

Import in `SecurityConfig`:

```java
@Import(TokenRealtyJwtAutoConfiguration.class)
```

Configure JWT (must match auth-service):

```yaml
tokenrealty:
  jwt:
    secret: ${JWT_SECRET:dev-secret-change-in-production-min-32-chars!!}
    issuer: tokenrealty-auth
```

For outbound service calls:

```yaml
services:
  auth:
    url: http://localhost:8083/api
tokenrealty:
  service-account:
    client-id: token-issuance
    client-secret: issuance-secret
```

RestClient beans can inject `ObjectProvider<ServiceTokenProvider>` to attach Bearer tokens.

## Build

```bash
./mvnw -pl tokenrealty-security install
```
