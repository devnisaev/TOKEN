import {
  loadStoredAuth,
  needsRefresh,
  saveStoredAuth,
  storedFromTokens,
  type StoredAuth,
} from '@tokenrealty/shared-api-client';
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import { api, setAccessTokenGetter } from '@/lib/api';
import type { TokenResponse, UserProfile } from '@/types/api';

const STORAGE_KEY = 'tokenrealty.auth';
const REFRESH_CHECK_MS = 30_000;

interface AuthContextValue {
  user: UserProfile | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  refreshProfile: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [stored, setStored] = useState<StoredAuth | null>(() => loadStoredAuth(STORAGE_KEY));
  const [user, setUser] = useState<UserProfile | null>(null);
  const [isLoading, setIsLoading] = useState(!!loadStoredAuth(STORAGE_KEY));
  const refreshInFlight = useRef<Promise<StoredAuth | null> | null>(null);

  const accessToken = stored?.accessToken ?? null;

  useEffect(() => {
    setAccessTokenGetter(() => stored?.accessToken ?? null);
  }, [stored]);

  const applyTokens = useCallback((tokens: TokenResponse) => {
    const next = storedFromTokens(tokens);
    saveStoredAuth(STORAGE_KEY, next);
    setStored(next);
    return next;
  }, []);

  const clearSession = useCallback(() => {
    saveStoredAuth(STORAGE_KEY, null);
    setStored(null);
    setUser(null);
  }, []);

  const refreshSession = useCallback(async (): Promise<StoredAuth | null> => {
    if (refreshInFlight.current) {
      return refreshInFlight.current;
    }
    const current = loadStoredAuth(STORAGE_KEY);
    if (!current?.refreshToken) {
      clearSession();
      return null;
    }
    refreshInFlight.current = (async () => {
      try {
        const tokens = await api.refresh(current.refreshToken);
        return applyTokens(tokens);
      } catch {
        clearSession();
        return null;
      } finally {
        refreshInFlight.current = null;
      }
    })();
    return refreshInFlight.current;
  }, [applyTokens, clearSession]);

  const refreshProfile = useCallback(async () => {
    if (!stored?.accessToken) {
      setUser(null);
      return;
    }
    try {
      const profile = await api.getProfile();
      setUser(profile);
    } catch {
      const renewed = await refreshSession();
      if (!renewed) return;
      const profile = await api.getProfile();
      setUser(profile);
    }
  }, [stored?.accessToken, refreshSession]);

  useEffect(() => {
    if (!stored?.accessToken) {
      setIsLoading(false);
      setUser(null);
      return;
    }
    refreshProfile()
      .catch(() => clearSession())
      .finally(() => setIsLoading(false));
  }, [stored?.accessToken, refreshProfile, clearSession]);

  useEffect(() => {
    if (!stored) return;
    const timer = window.setInterval(() => {
      if (needsRefresh(stored)) {
        refreshSession().catch(() => clearSession());
      }
    }, REFRESH_CHECK_MS);
    return () => window.clearInterval(timer);
  }, [stored, refreshSession, clearSession]);

  const login = useCallback(
    async (email: string, password: string) => {
      const tokens = await api.login(email, password);
      applyTokens(tokens);
      setIsLoading(true);
    },
    [applyTokens],
  );

  const logout = useCallback(() => {
    clearSession();
  }, [clearSession]);

  const value = useMemo(
    () => ({
      user,
      isAuthenticated: !!accessToken,
      isLoading,
      login,
      logout,
      refreshProfile,
    }),
    [user, accessToken, isLoading, login, logout, refreshProfile],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
