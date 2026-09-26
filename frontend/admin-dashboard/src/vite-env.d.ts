/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string;
  readonly VITE_USE_GRAPHQL_BFF?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
