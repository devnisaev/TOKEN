export type UserRole = 'ADMIN' | 'INVESTOR' | 'PROPERTY_MANAGER' | 'COMPLIANCE' | 'APPRAISER' | 'TENANT';

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
  userId: string;
  email: string;
  role: UserRole;
}

export interface UserProfile {
  id: string;
  email: string;
  role: UserRole;
  walletAddress: string | null;
  createdAt: string;
}

export interface SpringPage<T> {
  content: T[];
  totalElements: number;
}

export interface Building {
  id: string;
  name: string;
  address: string;
  city: string;
  country: string;
  status: string;
  flatCount: number;
  totalFlats?: number;
  propertyCategory?: string;
}

export interface BuildingDetail extends Building {
  postalCode?: string;
  totalFloors?: number;
  constructionYear?: number;
  totalAreaSqm?: number;
  cadastralReference?: string;
}

export interface CreateBuildingRequest {
  name: string;
  address: string;
  city: string;
  country: string;
  postalCode?: string;
  totalFloors?: number;
  totalFlats?: number;
  constructionYear?: number;
  totalAreaSqm?: number;
  propertyCategory?: string;
  cadastralReference?: string;
}

export interface UpdateBuildingRequest {
  name?: string;
  address?: string;
  city?: string;
  country?: string;
  postalCode?: string;
  totalFloors?: number;
  totalAreaSqm?: number;
  propertyCategory?: string;
  cadastralReference?: string;
}

export interface ComplianceRecord {
  id: string;
  investorId: string;
  walletAddress: string;
  fullName?: string;
  countryCode?: string;
  status: string;
  kycVerifiedAt?: string;
  kycExpiresAt?: string;
}

export interface DocumentReview {
  id: string;
  documentId: string;
  buildingId: string;
  flatId?: string;
  documentType: string;
  ipfsCid: string;
  status: string;
  isVerified: boolean;
  uploadedAt?: string;
  reviewedAt?: string;
}

export interface ApiError {
  title?: string;
  detail?: string;
  status?: number;
}
