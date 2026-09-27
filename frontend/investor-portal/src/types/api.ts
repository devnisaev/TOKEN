export type {
  ApiError,
  FlatDetailResponse,
  ListingDetailResponse,
  ListingSummary,
  PlaceOrderRequest,
  PlaceSellOrderRequest,
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

export interface BookLevel {
  priceUsd: number;
  totalQuantity: number;
  orderCount: number;
}

export interface BookDepthResponse {
  contractId: string;
  bids: BookLevel[];
  asks: BookLevel[];
  asOf: string;
}

export interface ExchangeOrder {
  id: string;
  contractId: string;
  flatId: string;
  buildingId: string;
  side: 'BID' | 'ASK';
  limitPriceUsd: number;
  originalQuantity: number;
  filledQuantity: number;
  remainingQuantity: number;
  status: string;
  investorId: string;
  fillsOnPlacement?: number;
}

export interface ExchangeTicker {
  contractId: string;
  lastPriceUsd: number | null;
  navPerTokenUsd: number | null;
  navDeltaPct: number | null;
  volume24hTokens: number;
  notional24hUsd: number;
  asOf: string;
}

export interface PlaceExchangeOrderRequest {
  contractId: string;
  flatId: string;
  buildingId: string;
  side: 'BID' | 'ASK';
  limitPriceUsd: number;
  quantity: number;
  investorId: string;
  walletAddress: string;
  liquidityTier?: string;
}

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

export interface Payout {
  id: string;
  recipientInvestorId: string;
  amount: number;
  grossAmountUsd?: number | null;
  withholdingAmountUsd?: number | null;
  purpose: string;
  period?: string | null;
  status: string;
  completedAt?: string | null;
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
  createdAt?: string | null;
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
  updatedAt?: string;
}

export interface NotificationPreferences {
  userId: string;
  emailEnabled: boolean;
  tradeAlerts: boolean;
  dividendAlerts: boolean;
  rentReminders: boolean;
}

export interface UpdateNotificationPreferencesRequest {
  emailEnabled: boolean;
  tradeAlerts: boolean;
  dividendAlerts: boolean;
  rentReminders: boolean;
}
