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

export interface ApiError {
  title?: string;
  detail?: string;
  status?: number;
}
