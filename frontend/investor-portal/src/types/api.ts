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
  totalPages: number;
  size: number;
  number: number;
}

export interface Listing {
  id: string;
  flatId: string;
  contractId: string;
  listingType: string;
  status: string;
  priceUsd: number;
  tokensAvailable: number;
  tokensTotal: number;
  minInvestmentTokens: number;
  title: string;
  description?: string;
  sellerInvestorId?: string;
  sellerWallet?: string;
  createdAt?: string;
}

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

export interface ListingDetailResponse {
  listing: ListingSummary;
  flatId: string;
  buildingName: string;
  flatNumber: string;
  flatStatus: string;
  tokenContract: TokenContractSummary | null;
}

export interface FlatDetailResponse {
  flatId: string;
  buildingId: string;
  buildingName: string;
  flatNumber: string;
  floor?: number;
  areaSqm?: number;
  status: string;
  tokenContract: TokenContractSummary | null;
  listing: ListingSummary | null;
}

export interface PlaceOrderRequest {
  listingId: string;
  buyerId: string;
  buyerWallet: string;
  tokenAmount: number;
}

export interface Order {
  id: string;
  listingId: string;
  flatId: string;
  contractId: string;
  orderType: string;
  status: string;
  buyerId: string;
  tokenAmount: number;
  totalPriceUsd: number;
}

export interface FiatBalance {
  currency: string;
  available: number;
  held: number;
}

export interface TokenHolding {
  contractId: string;
  tokenSymbol: string;
  walletAddress: string;
  balance: number;
}

export interface AggregateBalance {
  investorId: string;
  primaryWalletAddress: string | null;
  fiatBalances: FiatBalance[];
  tokenHoldings: TokenHolding[];
}

export interface ApiError {
  title?: string;
  detail?: string;
  status?: number;
}
