# Shared API Types (generated)

TypeScript types generated from OpenAPI specs via `openapi-typescript`.

## Regenerate

```bash
cd frontend/openapi
npm install
npm run codegen
```

Refresh specs from running springdoc (optional):

```bash
# Services must be running on default ports
npm run fetch-specs
npm run codegen
```

## Specs

Hand-maintained YAML in `frontend/openapi/specs/` — update when backend DTOs change, or use `fetch-specs` from live services.

Frontends import via `@tokenrealty/shared-api-client` (`frontend/shared-api-client/`), which re-exports these types. Each portal's `src/types/api.ts` adds required-field interfaces where openapi schemas are optional.
