export type {
  ApiError,
  FlatDetailResponse,
  ListingDetailResponse,
  ListingSummary,
  PlaceOrderRequest,
  SpringPage,
  TokenContractSummary,
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

export type {
  AggregateBalance,
  DividendPayment,
  FiatBalance,
  PortfolioBffDetail,
  TokenHolding,
} from '@tokenrealty/shared-api-client';

export interface Order {
  id: string;
  listingId: string;
  flatId: string;
  contractId: string;
  orderType: string;
  status: string;
  buyerId: string;
  sellerId?: string;
  buyerWallet: string;
  sellerWallet?: string;
  listingType: string;
  tokenAmount: number;
  totalPriceUsd: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface OrderStatusEvent {
  orderId: string;
  orderStatus: string;
  tradeStatus: string | null;
}

export interface Trade {
  id: string;
  orderId: string;
  listingId: string;
  flatId: string;
  contractId: string;
  buyerId: string;
  sellerId?: string;
  buyerWallet: string;
  listingType: string;
  tokenAmount: number;
  totalPriceUsd: number;
  status: string;
  paymentId?: string;
  transferId?: string;
  createdAt?: string;
}
