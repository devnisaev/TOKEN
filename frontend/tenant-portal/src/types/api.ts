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

export interface Lease {
  id: string;
  flatId: string;
  tenantId: string;
  tenantWallet: string;
  spvRecipientId: string;
  spvWallet: string;
  monthlyRentUsd: number;
  startDate: string;
  endDate?: string;
  status: string;
  createdAt?: string;
}

export interface RentPayment {
  id: string;
  leaseId: string;
  flatId: string;
  period: string;
  amount: number;
  payoutId?: string;
  status: string;
  paidAt?: string;
}

export interface RecordRentPaymentRequest {
  leaseId: string;
  period: string;
  amount: number;
}

export interface ApiError {
  title?: string;
  detail?: string;
  status?: number;
}
