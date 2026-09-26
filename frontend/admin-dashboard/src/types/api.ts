export type {
  ApiError,
  BuildingBffDetail,
  CreateBuildingRequest,
  CreateFlatRequest,
  SpringPage,
  TokenResponse,
  UpdateBuildingRequest,
  UpdateFlatRequest,
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
  flats?: FlatSummary[];
  spv?: SpvSummary | null;
}

export interface FlatSummary {
  id: string;
  flatNumber: string;
  floor?: number;
  areaSqm?: number;
  status: string;
  tokenPriceUsd?: number;
}

export interface SpvSummary {
  id: string;
  legalName?: string;
  registrationNumber?: string;
  kycVerified?: boolean;
  status?: string;
  walletAddress?: string;
}

export interface Flat {
  id: string;
  buildingId: string;
  buildingName?: string;
  flatNumber: string;
  floor?: number;
  areaSqm?: number;
  netUsableAreaSqm?: number;
  cadastralReference?: string;
  numRooms?: number;
  numBathrooms?: number;
  status?: string;
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

export interface Order {
  id: string;
  listingId: string;
  flatId?: string;
  contractId?: string;
  status: string;
  buyerId: string;
  sellerId?: string;
  buyerWallet?: string;
  sellerWallet?: string;
  listingType?: string;
  tokenAmount: number;
  totalPriceUsd: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface Trade {
  id: string;
  orderId: string;
  listingId?: string;
  flatId?: string;
  contractId?: string;
  buyerId?: string;
  sellerId?: string;
  buyerWallet?: string;
  sellerWallet?: string;
  listingType?: string;
  tokenAmount: number;
  totalPriceUsd: number;
  status: string;
  paymentId?: string;
  transferId?: string;
  createdAt?: string;
}
