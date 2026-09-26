import { createApiClient, resolveApiBaseUrl } from '@tokenrealty/shared-api-client';
import type {
  Lease,
  RecordRentPaymentRequest,
  RentPayment,
  TenantLeaseBffResponse,
  TokenResponse,
  UserProfile,
} from '@/types/api';

const client = createApiClient({
  baseUrl: resolveApiBaseUrl(import.meta.env.VITE_API_BASE_URL),
});

export function setAccessTokenGetter(getter: () => string | null) {
  client.setAccessTokenGetter(getter);
}

function request<T>(path: string, init: RequestInit = {}) {
  return client.request<T>(path, init);
}

export const api = {
  login(email: string, password: string) {
    return request<TokenResponse>('/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
  },

  refresh(refreshToken: string) {
    return request<TokenResponse>('/v1/auth/refresh', {
      method: 'POST',
      body: JSON.stringify({ refreshToken }),
    });
  },

  getProfile() {
    return request<UserProfile>('/v1/users/me');
  },

  listLeases(tenantId: string) {
    return request<Lease[]>(`/v1/leases?tenantId=${tenantId}`);
  },

  listTenantLeasesBff(tenantId: string) {
    return request<TenantLeaseBffResponse[]>(`/v1/bff/tenants/${tenantId}/lease`);
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
