export interface StoredAuth {
  accessToken: string;
  refreshToken: string;
  expiresAt: number;
  userId: string;
  email: string;
  role: string;
}

export interface TokenPayload {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  userId: string;
  email: string;
  role: string;
}

const DEFAULT_REFRESH_BUFFER_MS = 60_000;

export function storedFromTokens(tokens: TokenPayload): StoredAuth {
  return {
    accessToken: tokens.accessToken,
    refreshToken: tokens.refreshToken,
    expiresAt: Date.now() + tokens.expiresInSeconds * 1000,
    userId: tokens.userId,
    email: tokens.email,
    role: tokens.role,
  };
}

export function needsRefresh(stored: StoredAuth, bufferMs = DEFAULT_REFRESH_BUFFER_MS): boolean {
  return Date.now() >= stored.expiresAt - bufferMs;
}

export function loadStoredAuth(storageKey: string): StoredAuth | null {
  const raw = sessionStorage.getItem(storageKey);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as StoredAuth;
  } catch {
    return null;
  }
}

export function saveStoredAuth(storageKey: string, auth: StoredAuth | null): void {
  if (auth) {
    sessionStorage.setItem(storageKey, JSON.stringify(auth));
  } else {
    sessionStorage.removeItem(storageKey);
  }
}
