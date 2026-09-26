# Auth Service

Microservice for **TokenRealty** — JWT authentication, user registration, refresh tokens, and service accounts for inter-service calls.

Port **8083** · Database **`auth_service`**

## Responsibilities

- User registration and login (email + password)
- JWT access tokens (15 min) and refresh tokens (7 days)
- Service account tokens for internal microservices
- User profile and wallet linking

## Quick start

### 1. Create database

```bash
psql -U postgres -c "CREATE DATABASE auth_service;"
```

### 2. Run

```bash
./mvnw spring-boot:run
```

Set production secret:

```bash
JWT_SECRET=your-256-bit-secret-min-32-chars ./mvnw spring-boot:run
```

### 3. Swagger

```
http://localhost:8083/api/swagger-ui.html
```

## Dev users (seeded on startup)

| Email | Password | Role |
|-------|----------|------|
| admin@tokenrealty.com | admin123 | ADMIN |
| manager@tokenrealty.com | manager123 | PROPERTY_MANAGER |
| appraiser@tokenrealty.com | appraiser123 | APPRAISER |
| compliance@tokenrealty.com | compliance123 | COMPLIANCE |
| investor@tokenrealty.com | investor123 | INVESTOR |
| tenant@tokenrealty.com | tenant123 | TENANT |

## Service accounts (dev)

| clientId | clientSecret | Use |
|----------|--------------|-----|
| token-issuance | issuance-secret | Issuance → Registry calls |
| marketplace | marketplace-secret | Marketplace internal calls |

## API

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/v1/auth/register` | Public | Register INVESTOR/TENANT |
| POST | `/v1/auth/login` | Public | Login → tokens |
| POST | `/v1/auth/refresh` | Public | Refresh access token |
| POST | `/v1/auth/service-token` | Public | Service account JWT |
| GET | `/v1/users/me` | Bearer | Current profile |
| PATCH | `/v1/users/me/wallet` | Bearer | Link wallet address |

## Example

```bash
# Login
curl -s -X POST http://localhost:8083/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"investor@tokenrealty.com","password":"investor123"}'

# Use access token
curl -s http://localhost:8083/api/v1/users/me \
  -H "Authorization: Bearer <accessToken>"

# Service token (inter-service)
curl -s -X POST http://localhost:8083/api/v1/auth/service-token \
  -H 'Content-Type: application/json' \
  -d '{"clientId":"token-issuance","clientSecret":"issuance-secret"}'
```

## JWT claims

```json
{
  "sub": "user-uuid",
  "email": "investor@tokenrealty.com",
  "role": "INVESTOR",
  "service": false,
  "iss": "tokenrealty-auth",
  "exp": "..."
}
```

## Tests

```bash
./mvnw test
```

## Next steps

- [x] Shared `tokenrealty-security` library for other services
- [x] Migrate registry/issuance/marketplace from Basic auth to JWT validation
- [x] Wire `PropertyRegistryClient` / `TokenIssuanceClient` with service-token Bearer header
- [ ] OAuth2 / social login (optional)

Conventions: [`.cursor/rules/spring-java-services.mdc`](../.cursor/rules/spring-java-services.mdc)
