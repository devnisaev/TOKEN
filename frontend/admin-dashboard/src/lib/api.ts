import type {
  Building,
  BuildingDetail,
  ComplianceRecord,
  CreateBuildingRequest,
  DocumentReview,
  SpringPage,
  TokenResponse,
  UpdateBuildingRequest,
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

  listBuildings() {
    return request<SpringPage<Building>>('/v1/buildings?size=50&sort=name,asc');
  },

  getBuilding(id: string) {
    return request<BuildingDetail>(`/v1/buildings/${id}`);
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
};
