# TokenRealty — Tenant Portal

React + TypeScript tenant frontend for viewing leases and paying rent.

## Run

```bash
cd frontend/tenant-portal
npm install
npm run dev
```

Open [http://localhost:5175](http://localhost:5175)

## Demo login

| Email | Password |
|-------|----------|
| `tenant@tokenrealty.com` | `tenant123` |

Requires API Gateway `:8080`, Auth, and Rental Service.

## Pages

| Route | Description |
|-------|-------------|
| `/login` | Tenant JWT sign-in |
| `/` | Lease details + pay rent + payment history |
