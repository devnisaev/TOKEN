export interface ProblemDetail {
  title?: string;
  detail?: string;
  status?: number;
}

export interface ApiClientOptions {
  baseUrl?: string;
  getAccessToken?: () => string | null;
}

export interface ApiClient {
  request<T>(path: string, init?: RequestInit): Promise<T>;
  getBaseUrl(): string;
  setAccessTokenGetter(getter: () => string | null): void;
}

export function createApiClient(options: ApiClientOptions = {}): ApiClient {
  let baseUrl = options.baseUrl ?? '/api';
  let getAccessToken = options.getAccessToken ?? (() => null);

  async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
    const headers = new Headers(init.headers);
    if (!headers.has('Content-Type') && init.body) {
      headers.set('Content-Type', 'application/json');
    }

    const token = getAccessToken();
    if (token) {
      headers.set('Authorization', `Bearer ${token}`);
    }

    const response = await fetch(`${baseUrl}${path}`, { ...init, headers });

    if (!response.ok) {
      let detail = response.statusText;
      try {
        const body = (await response.json()) as ProblemDetail;
        detail = body.detail ?? body.title ?? detail;
      } catch {
        /* empty body */
      }
      throw new Error(detail);
    }

    if (response.status === 204) {
      return undefined as T;
    }

    return response.json() as Promise<T>;
  }

  return {
    request,
    getBaseUrl: () => baseUrl,
    setAccessTokenGetter(getter) {
      getAccessToken = getter;
    },
  };
}

/** Resolve API base URL from Vite env or default proxy prefix. */
export function resolveApiBaseUrl(viteEnvBase?: string): string {
  return viteEnvBase ?? '/api';
}
