export type {
  ApiError,
  RecordRentPaymentRequest,
  TokenResponse,
} from '@tokenrealty/shared-api-client';

import type { UserRole } from '@tokenrealty/shared-api-client';

export type { UserRole };

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

export interface TenantFlatSummary {
  flatId: string;
  buildingId: string;
  buildingName: string;
  flatNumber: string;
  floor?: number;
  areaSqm?: number;
  status: string;
}

export interface TenantLeaseBffResponse {
  lease: Lease;
  flat: TenantFlatSummary;
  rentDue: boolean;
}
