import type { ProblemDetail } from './client';
import type { components as AuthComponents } from '../../shared-api-types/auth';
import type { components as ComplianceComponents } from '../../shared-api-types/compliance';
import type { components as GatewayComponents } from '../../shared-api-types/gateway';
import type { components as IndexerComponents } from '../../shared-api-types/indexer';
import type { components as IssuanceComponents } from '../../shared-api-types/issuance';
import type { components as MarketplaceComponents } from '../../shared-api-types/marketplace';
import type { components as NotificationComponents } from '../../shared-api-types/notification';
import type { components as PaymentComponents } from '../../shared-api-types/payment';
import type { components as RegistryComponents } from '../../shared-api-types/registry';
import type { components as RentalComponents } from '../../shared-api-types/rental';
import type { components as WalletComponents } from '../../shared-api-types/wallet';

type Auth = AuthComponents['schemas'];
type Compliance = ComplianceComponents['schemas'];
type Gateway = GatewayComponents['schemas'];
type Indexer = IndexerComponents['schemas'];
type Issuance = IssuanceComponents['schemas'];
type Marketplace = MarketplaceComponents['schemas'];
type Notification = NotificationComponents['schemas'];
type Payment = PaymentComponents['schemas'];
type Registry = RegistryComponents['schemas'];
type Rental = RentalComponents['schemas'];
type Wallet = WalletComponents['schemas'];

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
export type PlaceSellOrderRequest = Marketplace['PlaceSellOrderRequest'];
export type SettleTradeRequest = Marketplace['SettleTradeRequest'];

export type Lease = Rental['Lease'] & { id: string };
export type RentPayment = Rental['RentPayment'] & { id: string };
export type RecordRentPaymentRequest = Rental['RecordRentPaymentRequest'];
export type MaintenanceTicket = Rental['MaintenanceTicket'] & { id: string };
export type CreateMaintenanceTicketRequest = Rental['CreateMaintenanceTicketRequest'];
export type UpdateMaintenanceTicketRequest = Rental['UpdateMaintenanceTicketRequest'];

export type PaymentRecord = Payment['Payment'] & { id: string };
export type InitiatePaymentRequest = Payment['InitiatePaymentRequest'];
export type ConfirmPaymentRequest = Payment['ConfirmPaymentRequest'];
export type Escrow = Payment['Escrow'] & { id: string };

export type WalletRecord = Wallet['Wallet'] & { id: string };
export type CreateCustodialWalletRequest = Wallet['CreateCustodialWalletRequest'];
export type LinkWalletRequest = Wallet['LinkWalletRequest'];

export type NotificationPreferences = Notification['NotificationPreferences'];
export type UpdateNotificationPreferencesRequest = Notification['UpdateNotificationPreferencesRequest'];

export type IssueTokenRequest = Issuance['IssueTokenRequest'];
export type TokenContract = Issuance['TokenContract'] & { id: string };
export type TokenHoldingRecord = Issuance['TokenHolding'];

export type IndexerStatusResponse = Indexer['IndexerStatusResponse'];
export type IndexedEvent = Indexer['IndexedEvent'] & { id: string };
export type IndexedEventPage = Indexer['IndexedEventPage'];
export type ReconciliationMismatch = Indexer['ReconciliationMismatch'] & { id: string };

/** BFF aggregates (gateway OpenAPI). */
export type TokenContractSummary = Gateway['TokenContractSummary'];
export type ListingSummary = Required<Pick<Gateway['ListingSummary'], 'listingId' | 'listingType' | 'status' | 'priceUsd' | 'tokensAvailable' | 'tokensTotal' | 'minInvestmentTokens' | 'title'>>;
export type FlatDetailResponse = Required<Pick<Gateway['FlatDetailResponse'], 'flatId' | 'buildingId' | 'buildingName' | 'flatNumber' | 'status'>> & Gateway['FlatDetailResponse'];
export type ListingDetailResponse = Required<Pick<Gateway['ListingDetailResponse'], 'flatId' | 'buildingName' | 'flatNumber' | 'flatStatus'>> & {
  listing: ListingSummary;
  tokenContract?: TokenContractSummary | null;
};
export type OrderStatusEvent = Required<Pick<Gateway['OrderStatusEvent'], 'orderId' | 'orderStatus'>> & Gateway['OrderStatusEvent'];
export type TenantFlatSummary = Gateway['TenantFlatSummary'];
export type TenantLeaseBffResponse = Required<Pick<Gateway['TenantLeaseBffResponse'], 'lease' | 'flat' | 'rentDue'>>;
export type MaintenanceTicketView = Gateway['MaintenanceTicketView'] & { id: string };
export type TenantMaintenanceBffResponse = Required<Pick<Gateway['TenantMaintenanceBffResponse'], 'lease' | 'flat' | 'openTickets'>>;

export interface BuildingBffDetail {
  building: BuildingDetail;
  tokenizedFlatCount: number;
  availableFlatCount: number;
}

export interface FiatBalance {
  currency: string;
  available: number;
  held: number;
}

export type TokenHolding = Required<Pick<Gateway['EnrichedTokenHoldingView'], 'contractId' | 'tokenSymbol' | 'walletAddress' | 'balance'>> &
  Gateway['EnrichedTokenHoldingView'];

export interface AggregateBalance {
  investorId: string;
  primaryWalletAddress: string | null;
  fiatBalances: FiatBalance[];
  tokenHoldings: TokenHolding[];
}

export interface DividendPayment {
  id: string;
  contractId: string;
  investorId: string;
  investorWallet?: string;
  periodStart?: string;
  periodEnd?: string;
  tokensHeld?: number;
  ownershipPct?: number;
  grossRentalIncomeUsd?: number;
  amountUsd: number;
  txHash?: string;
  paidAt?: string;
  status: string;
  createdAt?: string;
}

export interface PortfolioBffDetail {
  balance: AggregateBalance;
  recentDividends: DividendPayment[];
}

export type ApiError = ProblemDetail;
