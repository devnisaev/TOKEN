export type {
  ApiError,
  CreateFlatRequest,
  MaintenanceTicket,
  SettleTradeRequest,
  SpringPage,
  TokenResponse,
  UpdateBuildingRequest,
  UpdateFlatRequest,
  UpdateMaintenanceTicketRequest,
} from '@tokenrealty/shared-api-client';

import type { CreateBuildingRequest as BaseCreateBuildingRequest, UserRole } from '@tokenrealty/shared-api-client';

export type { UserRole };

/** Registry create/update payload including Phase 4b-3 metadata fields. */
export interface CreateBuildingRequest extends BaseCreateBuildingRequest {
  energyEfficiencyRating?: string;
  zoningCode?: string;
  lastRenovationYear?: number;
}

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

export interface BuildingBffDetail {
  building: BuildingDetail;
  tokenizedFlatCount: number;
  availableFlatCount: number;
  approvedValuationCount?: number;
  latestNavAttestedAt?: string | null;
}

export interface GovernanceProposal {
  id: string;
  flatId: string;
  title: string;
  description: string;
  status: string;
  quorumPct: number;
  votesFor: number;
  votesAgainst: number;
  closesAt: string;
  closedAt?: string | null;
}

export interface IntegrationCredential {
  id: string;
  integrationType: string;
  provider: string;
  version: number;
  rotatedAt?: string | null;
  createdAt?: string | null;
}

export interface IntegrationDelivery {
  id: string;
  integrationType: string;
  provider: string;
  status: string;
  attempts: number;
  lastError?: string | null;
  createdAt?: string | null;
}

export interface ComplianceReport {
  taxSummaries: TaxSummaryItem[];
  taxSummaryTotal: number;
  surveillanceAlerts: SurveillanceAlertItem[];
  surveillanceAlertTotal: number;
}

export interface TaxSummaryItem {
  id: string;
  payoutId: string;
  recipientInvestorId: string;
  grossAmountUsd: number;
  withholdingAmountUsd: number;
  netAmountUsd: number;
  completedAt: string;
}

export interface CreateGovernanceProposalRequest {
  flatId: string;
  title: string;
  description: string;
  quorumPct: number;
  closesAt: string;
}

export interface SurveillanceAlertItem {
  id: string;
  orderId: string;
  buyerId?: string | null;
  sellerId?: string | null;
  alertType: string;
  detectedAt: string;
}

export interface BuildingDetail extends Building {
  postalCode?: string;
  totalFloors?: number;
  constructionYear?: number;
  totalAreaSqm?: number;
  cadastralReference?: string;
  energyEfficiencyRating?: string;
  zoningCode?: string;
  lastRenovationYear?: number;
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
  orderType?: string;
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
  updatedAt?: string;
}

export interface IssueTokenRequest {
  flatId: string;
  buildingId: string;
  tokenName: string;
  tokenSymbol: string;
  totalSupply: number;
  tokenPriceUsd: number;
  spvWalletAddress: string;
}

export interface TokenContract {
  id: string;
  flatId: string;
  buildingId: string;
  tokenName: string;
  tokenSymbol: string;
  totalSupply: number;
  tokenPriceUsd: number;
  contractAddress?: string;
  status: string;
}
