import type {
  AggregateBalance,
  ApiError,
  FlatDetailResponse,
  Listing,
  ListingDetailResponse,
  Order,
  PlaceOrderRequest,
  Trade,
  SpringPage,
  TokenResponse,
  UserProfile,
} from '@/types/api';

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '/api';

type TokenGetter = () => string | null;

let getAccessToken: TokenGetter = () => null;

export function setAccessTokenGetter(getter: TokenGetter) {
  getAccessToken = getter;
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set('Content-Type', 'application/json');

  const token = getAccessToken();
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE}${path}`, { ...init, headers });

  if (!response.ok) {
    let error: ApiError = { status: response.status, detail: response.statusText };
    try {
      error = await response.json();
    } catch {
      /* empty body */
    }
    throw new Error(error.detail ?? error.title ?? `Request failed (${response.status})`);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
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

  getPortfolio(investorId: string) {
    return request<AggregateBalance>(`/v1/wallets/${investorId}/balance`);
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
};
