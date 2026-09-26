import type {
  Lease,
  RecordRentPaymentRequest,
  RentPayment,
  TokenResponse,
  UserProfile,
} from '@/types/api';

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '/api';

type TokenGetter = () => string | null;

let getAccessToken: TokenGetter = () => null;

export function setAccessTokenGetter(getter: TokenGetter) {
  getAccessToken = getter;
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set('Content-Type', 'application/json');

  const token = getAccessToken();
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE}${path}`, { ...init, headers });

  if (!response.ok) {
    let detail = response.statusText;
    try {
      const body = await response.json();
      detail = body.detail ?? body.title ?? detail;
    } catch {
      /* empty */
    }
    throw new Error(detail);
  }

  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export const api = {
  login(email: string, password: string) {
    return request<TokenResponse>('/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
  },

  getProfile() {
    return request<UserProfile>('/v1/users/me');
  },

  listLeases(tenantId: string) {
    return request<Lease[]>(`/v1/leases?tenantId=${tenantId}`);
  },

  getLease(leaseId: string) {
    return request<Lease>(`/v1/leases/${leaseId}`);
  },

  listRentPayments(leaseId: string) {
    return request<RentPayment[]>(`/v1/rent-payments?leaseId=${leaseId}`);
  },

  payRent(body: RecordRentPaymentRequest) {
    return request<RentPayment>('/v1/rent-payments', {
      method: 'POST',
      body: JSON.stringify(body),
    });
  },
};
