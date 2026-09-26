import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { loadStoredAuth, subscribeSse, resolveApiBaseUrl } from '@tokenrealty/shared-api-client';
import { ArrowLeft, Loader2 } from 'lucide-react';
import { api } from '@/lib/api';
import { formatUsd } from '@/lib/utils';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { OrderStatusTimeline, SettlementAuditTrail } from '@tokenrealty/shared-ui';
import type { Order, OrderStatusEvent, Trade } from '@/types/api';

async function fetchTrade(orderId: string) {
  try {
    return await api.getOrderTrade(orderId);
  } catch {
    return null;
  }
}

function isTerminal(orderStatus: string, tradeStatus?: string) {
  return (
    orderStatus === 'SETTLED' ||
    orderStatus === 'CANCELLED' ||
    tradeStatus === 'SETTLED' ||
    tradeStatus === 'FAILED'
  );
}

function applyStatusEvent(order: Order, event: OrderStatusEvent): Order {
  return order.status === event.orderStatus ? order : { ...order, status: event.orderStatus };
}

function applyTradeEvent(trade: Trade | null, event: OrderStatusEvent): Trade | null {
  if (!event.tradeStatus) {
    return trade;
  }
  if (trade) {
    return trade.status === event.tradeStatus ? trade : { ...trade, status: event.tradeStatus };
  }
  return {
    id: '',
    orderId: event.orderId,
    listingId: '',
    flatId: '',
    contractId: '',
    buyerId: '',
    buyerWallet: '',
    listingType: '',
    tokenAmount: 0,
    totalPriceUsd: 0,
    status: event.tradeStatus,
  };
}

const AUTH_STORAGE_KEY = 'tokenrealty.auth';

export function OrderStatusPage() {
  const { orderId } = useParams<{ orderId: string }>();
  const queryClient = useQueryClient();
  const [streaming, setStreaming] = useState(true);

  const orderQuery = useQuery({
    queryKey: ['order', orderId],
    queryFn: () => api.getOrder(orderId!),
    enabled: !!orderId,
  });

  const tradeQuery = useQuery({
    queryKey: ['order-trade', orderId],
    queryFn: () => fetchTrade(orderId!),
    enabled: !!orderId,
  });

  useEffect(() => {
    if (!orderId) {
      return;
    }

    const controller = new AbortController();
    const baseUrl = resolveApiBaseUrl(import.meta.env.VITE_API_BASE_URL);

    (async () => {
      try {
        for await (const message of subscribeSse<OrderStatusEvent>({
          baseUrl,
          path: `/v1/bff/orders/${orderId}/status-stream`,
          getAccessToken: () => loadStoredAuth(AUTH_STORAGE_KEY)?.accessToken ?? null,
          signal: controller.signal,
        })) {
          const event = message.data;
          queryClient.setQueryData<Order>(['order', orderId], (current) =>
            current ? applyStatusEvent(current, event) : current,
          );
          queryClient.setQueryData<Trade | null>(['order-trade', orderId], (current) =>
            applyTradeEvent(current ?? null, event),
          );
          if (isTerminal(event.orderStatus, event.tradeStatus ?? undefined)) {
            setStreaming(false);
            break;
          }
        }
      } catch (error) {
        if (!controller.signal.aborted) {
          setStreaming(false);
        }
      }
    })();

    return () => controller.abort();
  }, [orderId, queryClient]);

  const order = orderQuery.data;
  const trade = tradeQuery.data ?? null;
  const loading = orderQuery.isLoading;
  const polling = streaming && (!order || !isTerminal(order.status, trade?.status));

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20 text-muted-foreground">
        <Loader2 className="mr-2 h-5 w-5 animate-spin" />
        Loading order…
      </div>
    );
  }

  if (orderQuery.error || !order) {
    return (
      <div className="space-y-4">
        <Link to="/orders" className="inline-flex items-center text-sm text-muted-foreground">
          <ArrowLeft className="mr-1 h-4 w-4" /> My orders
        </Link>
        <p className="text-destructive">Order not found</p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-lg space-y-6">
      <Link to="/orders" className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="mr-1 h-4 w-4" /> My orders
      </Link>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Order {order.id.slice(0, 8)}…</CardTitle>
          <p className="text-sm text-muted-foreground">
            {order.tokenAmount.toLocaleString()} tokens · {formatUsd(order.totalPriceUsd)}
          </p>
          <Link
            to={`/listings/${order.listingId}`}
            className="text-xs text-primary underline-offset-4 hover:underline"
          >
            View listing
          </Link>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="flex justify-between text-sm">
            <span className="text-muted-foreground">Order status</span>
            <span className="font-medium">{order.status}</span>
          </div>
          <OrderStatusTimeline orderStatus={order.status} trade={trade} polling={polling} />
          <SettlementAuditTrail order={order} trade={trade} />
        </CardContent>
      </Card>
    </div>
  );
}
