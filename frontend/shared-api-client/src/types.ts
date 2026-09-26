import type { ProblemDetail } from './client';
import type { components as AuthComponents } from '../../shared-api-types/auth';
import type { components as ComplianceComponents } from '../../shared-api-types/compliance';
import type { components as MarketplaceComponents } from '../../shared-api-types/marketplace';
import type { components as RegistryComponents } from '../../shared-api-types/registry';
import type { components as RentalComponents } from '../../shared-api-types/rental';

type Auth = AuthComponents['schemas'];
type Compliance = ComplianceComponents['schemas'];
type Marketplace = MarketplaceComponents['schemas'];
type Registry = RegistryComponents['schemas'];
type Rental = RentalComponents['schemas'];

export type UserRole = 'ADMIN' | 'INVESTOR' | 'PROPERTY_MANAGER' | 'COMPLIANCE' | 'APPRAISER' | 'TENANT';

export type TokenResponse = Auth['TokenResponse'];
export type UserProfile = Auth['UserProfile'];

export type SpringPage<T> = {
  content: T[];
  totalElements: number;
  totalPages?: number;
  size?: number;
  number?: number;
};

export type Building = Registry['BuildingResponse'] & { id: string };
export type BuildingDetail = Registry['BuildingDetailResponse'] & {
  id: string;
  flats?: FlatSummary[];
  spv?: SpvSummary | null;
};
export type CreateBuildingRequest = Registry['CreateBuildingRequest'];
export type UpdateBuildingRequest = Registry['UpdateBuildingRequest'];
export type FlatSummary = Registry['FlatSummary'] & { id: string };
export type Flat = Registry['FlatResponse'] & { id: string; buildingId: string };
export type CreateFlatRequest = Registry['CreateFlatRequest'];
export type UpdateFlatRequest = Registry['UpdateFlatRequest'];
export type SpvSummary = Registry['SpvSummary'];

export type ComplianceRecord = Compliance['ComplianceRecord'];
export type DocumentReview = Compliance['DocumentReview'];

export type Listing = Marketplace['Listing'] & { id: string; flatId: string };
export type Order = Marketplace['Order'] & { id: string; listingId: string; buyerId: string };
export type Trade = Marketplace['Trade'] & { id: string; orderId: string };
export type PlaceOrderRequest = Marketplace['PlaceOrderRequest'];

export type Lease = Rental['Lease'] & { id: string };
export type RentPayment = Rental['RentPayment'] & { id: string };
export type RecordRentPaymentRequest = Rental['RecordRentPaymentRequest'];

/** BFF aggregates (gateway) — hand-maintained until gateway OpenAPI spec exists. */
export interface TokenContractSummary {
  contractId?: string;
  contractAddress?: string;
  tokenSymbol?: string;
  totalSupply?: number;
  tokenPriceUsd?: number;
  status?: string;
}

export interface ListingSummary {
  listingId: string;
  listingType: string;
  status: string;
  priceUsd: number;
  tokensAvailable: number;
  tokensTotal: number;
  minInvestmentTokens: number;
  title: string;
}

export interface FlatDetailResponse {
  flatId: string;
  buildingId: string;
  buildingName: string;
  flatNumber: string;
  floor?: number;
  areaSqm?: number;
  status: string;
  tokenContract?: TokenContractSummary | null;
  listing?: ListingSummary | null;
}

export interface ListingDetailResponse {
  listing: ListingSummary;
  flatId: string;
  buildingName: string;
  flatNumber: string;
  flatStatus: string;
  tokenContract?: TokenContractSummary | null;
}

export interface BuildingBffDetail {
  building: BuildingDetail;
  tokenizedFlatCount: number;
  availableFlatCount: number;
}

export interface AggregateBalance {
  investorId: string;
  totalUsd: number;
  walletBalanceUsd: number;
  tokenHoldingsUsd: number;
  holdings?: Array<{
    flatId: string;
    tokenSymbol?: string;
    balance: number;
    valueUsd: number;
  }>;
}

export type ApiError = ProblemDetail;
