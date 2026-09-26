# Shared API Client — TokenRealty Frontends

Common `fetch` wrapper and OpenAPI-derived types for all React portals.

## Usage

Each portal depends on this package via `"@tokenrealty/shared-api-client": "file:../shared-api-client"`.

```typescript
import { createApiClient, resolveApiBaseUrl } from '@tokenrealty/shared-api-client';
import type { TokenResponse } from '@tokenrealty/shared-api-client';

const client = createApiClient({
  baseUrl: resolveApiBaseUrl(import.meta.env.VITE_API_BASE_URL),
});

client.setAccessTokenGetter(() => sessionStorage.getItem('accessToken'));

const profile = await client.request<UserProfile>('/v1/users/me');
```

Portal-specific DTOs with required fields live in each app's `src/types/api.ts` — re-export shared types where openapi schemas are loose.

## Types source

Generated types: [../shared-api-types/](../shared-api-types/) via `cd ../openapi && npm run codegen`.

BFF shapes (`BuildingBffDetail`, `FlatDetailResponse`, …) are hand-maintained in `src/types.ts` until a gateway OpenAPI spec exists.
