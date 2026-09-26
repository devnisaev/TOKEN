import { createApiClient, resolveApiBaseUrl } from '@tokenrealty/shared-api-client';
import type {
  Building,
  BuildingBffDetail,
  BuildingDetail,
  ComplianceRecord,
  CreateBuildingRequest,
  CreateFlatRequest,
  DocumentReview,
  Flat,
  Order,
  Trade,
  SpringPage,
  TokenResponse,
  UpdateBuildingRequest,
  UpdateFlatRequest,
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

  listBuildings() {
    return request<SpringPage<Building>>('/v1/buildings?size=50&sort=name,asc');
  },

  getBuilding(id: string) {
    return request<BuildingDetail>(`/v1/buildings/${id}`);
  },

  getBuildingBff(id: string) {
    return request<BuildingBffDetail>(`/v1/bff/buildings/${id}`);
  },

  createBuilding(body: CreateBuildingRequest) {
    return request<Building>('/v1/buildings', {
      method: 'POST',
      body: JSON.stringify(body),
    });
  },

  updateBuilding(id: string, body: UpdateBuildingRequest) {
    return request<Building>(`/v1/buildings/${id}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    });
  },

  listFlats(buildingId: string) {
    return request<SpringPage<Flat>>(`/v1/buildings/${buildingId}/flats?size=50&sort=flatNumber,asc`);
  },

  getFlat(flatId: string) {
    return request<Flat>(`/v1/flats/${flatId}`);
  },

  createFlat(buildingId: string, body: CreateFlatRequest) {
    return request<Flat>(`/v1/buildings/${buildingId}/flats`, {
      method: 'POST',
      body: JSON.stringify(body),
    });
  },

  updateFlat(flatId: string, body: UpdateFlatRequest) {
    return request<Flat>(`/v1/flats/${flatId}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    });
  },

  listCompliance(status?: string) {
    const q = status ? `&status=${status}` : '';
    return request<SpringPage<ComplianceRecord>>(`/v1/compliance?size=50${q}`);
  },

  verifyCompliance(id: string, kycExpiresAt: string) {
    return request<ComplianceRecord>(`/v1/compliance/${id}/verify`, {
      method: 'PATCH',
      body: JSON.stringify({ kycExpiresAt }),
    });
  },

  listPendingDocumentReviews() {
    return request<DocumentReview[]>('/v1/compliance/document-reviews/pending');
  },

  verifyDocumentReview(documentId: string) {
    return request<DocumentReview>(`/v1/compliance/document-reviews/${documentId}/verify`, {
      method: 'PATCH',
    });
  },

  listOrders() {
    return request<SpringPage<Order>>('/v1/orders?size=50&sort=createdAt,desc');
  },

  getOrder(orderId: string) {
    return request<Order>(`/v1/orders/${orderId}`);
  },

  getOrderTrade(orderId: string) {
    return request<Trade>(`/v1/orders/${orderId}/trade`);
  },
};
