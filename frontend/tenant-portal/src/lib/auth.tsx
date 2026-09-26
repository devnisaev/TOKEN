import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { api, setAccessTokenGetter } from '@/lib/api';
import type { TokenResponse, UserProfile } from '@/types/api';

const STORAGE_KEY = 'tokenrealty.tenant.auth';

interface StoredAuth {
  accessToken: string;
  refreshToken: string;
  userId: string;
  email: string;
  role: string;
}

interface AuthContextValue {
  user: UserProfile | null;
  isAuthenticated: boolean;
  isTenant: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function loadStored(): StoredAuth | null {
  const raw = sessionStorage.getItem(STORAGE_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as StoredAuth;
  } catch {
    return null;
  }
}

function saveStored(auth: StoredAuth | null) {
  if (auth) sessionStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
  else sessionStorage.removeItem(STORAGE_KEY);
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [stored, setStored] = useState<StoredAuth | null>(() => loadStored());
  const [user, setUser] = useState<UserProfile | null>(null);
  const [isLoading, setIsLoading] = useState(!!loadStored());

  const accessToken = stored?.accessToken ?? null;

  useEffect(() => {
    setAccessTokenGetter(() => accessToken);
  }, [accessToken]);

  useEffect(() => {
    if (!accessToken) {
      setIsLoading(false);
      setUser(null);
      return;
    }
    api.getProfile()
      .then(setUser)
      .catch(() => {
        saveStored(null);
        setStored(null);
        setUser(null);
      })
      .finally(() => setIsLoading(false));
  }, [accessToken]);

  const login = useCallback(async (email: string, password: string) => {
    const tokens: TokenResponse = await api.login(email, password);
    if (tokens.role !== 'TENANT') {
      throw new Error('Tenant role required');
    }
    const next: StoredAuth = {
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      userId: tokens.userId,
      email: tokens.email,
      role: tokens.role,
    };
    saveStored(next);
    setStored(next);
    setIsLoading(true);
  }, []);

  const logout = useCallback(() => {
    saveStored(null);
    setStored(null);
    setUser(null);
  }, []);

  const isTenant = user?.role === 'TENANT';

  const value = useMemo(
    () => ({
      user,
      isAuthenticated: !!accessToken,
      isTenant,
      isLoading,
      login,
      logout,
    }),
    [user, accessToken, isTenant, isLoading, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
