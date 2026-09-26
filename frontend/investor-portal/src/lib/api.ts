import { createApiClient, resolveApiBaseUrl } from '@tokenrealty/shared-api-client';
import type {
  DividendPayment,
  FlatDetailResponse,
  Listing,
  ListingDetailResponse,
  Order,
  PlaceOrderRequest,
  PlaceSellOrderRequest,
  PortfolioBffDetail,
  SpringPage,
  TokenResponse,
  Trade,
  NotificationPreferences,
  UpdateNotificationPreferencesRequest,
  UserProfile,
} from '@/types/api';

const client = createApiClient({
  baseUrl: resolveApiBaseUrl(import.meta.env.VITE_API_BASE_URL),
});

export function setAccessTokenGetter(getter: () => string | null) {
  client.setAccessTokenGetter(getter);
}

function request<T>(path: string, init: RequestInit = {}) {
  return client.request<T>(path, init);
}

export const api = {
  login(email: string, password: string) {
    return request<TokenResponse>('/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
  },

  refresh(refreshToken: string) {
    return request<TokenResponse>('/v1/auth/refresh', {
      method: 'POST',
      body: JSON.stringify({ refreshToken }),
    });
  },

  getProfile() {
    return request<UserProfile>('/v1/users/me');
  },

  listListings(status = 'ACTIVE') {
    return request<SpringPage<Listing>>(
      `/v1/listings?status=${status}&size=50&sort=createdAt,desc`,
    );
  },

  getListingDetail(listingId: string) {
    return request<ListingDetailResponse>(`/v1/bff/listings/${listingId}`);
  },

  getFlatDetail(flatId: string) {
    return request<FlatDetailResponse>(`/v1/bff/flats/${flatId}`);
  },

  placeBuyOrder(body: PlaceOrderRequest) {
    return request<Order>('/v1/orders', {
      method: 'POST',
      body: JSON.stringify(body),
    });
  },

  placeSellOrder(body: PlaceSellOrderRequest) {
    return request<Order>('/v1/orders/sell', {
      method: 'POST',
      body: JSON.stringify(body),
    });
  },

  listOrders(buyerId: string) {
    return request<SpringPage<Order>>(
      `/v1/orders?buyerId=${buyerId}&size=50&sort=createdAt,desc`,
    );
  },

  getOrder(orderId: string) {
    return request<Order>(`/v1/orders/${orderId}`);
  },

  getOrderTrade(orderId: string) {
    return request<Trade>(`/v1/orders/${orderId}/trade`);
  },

  getPortfolioBff(investorId: string) {
    return request<PortfolioBffDetail>(`/v1/bff/investors/${investorId}/portfolio`);
  },

  listDividends(investorId: string) {
    return request<DividendPayment[]>(`/v1/investors/${investorId}/dividends`);
  },

  linkWallet(investorId: string, walletAddress: string) {
    return request('/v1/wallets/link', {
      method: 'POST',
      body: JSON.stringify({
        investorId,
        walletAddress,
        label: 'MetaMask',
        primary: true,
      }),
    });
  },

  updateProfileWallet(walletAddress: string) {
    return request<UserProfile>('/v1/users/me/wallet', {
      method: 'PATCH',
      body: JSON.stringify({ walletAddress }),
    });
  },

  getNotificationPreferences(userId: string) {
    return request<NotificationPreferences>(`/v1/notifications/preferences/${userId}`);
  },

  updateNotificationPreferences(userId: string, body: UpdateNotificationPreferencesRequest) {
    return request<NotificationPreferences>(`/v1/notifications/preferences/${userId}`, {
      method: 'PATCH',
      body: JSON.stringify(body),
    });
  },
};
