import { Link, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ArrowLeft, Loader2 } from 'lucide-react';
import { api } from '@/lib/api';
import { formatUsd } from '@/lib/utils';
import { OrderStatusTimeline } from '@/components/OrderStatusTimeline';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

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

export function OrderStatusPage() {
  const { orderId } = useParams<{ orderId: string }>();

  const orderQuery = useQuery({
    queryKey: ['order', orderId],
    queryFn: () => api.getOrder(orderId!),
    enabled: !!orderId,
    refetchInterval: (q) => {
      const o = q.state.data;
      if (!o) return 3000;
      return isTerminal(o.status) ? false : 3000;
    },
  });

  const tradeQuery = useQuery({
    queryKey: ['order-trade', orderId],
    queryFn: () => fetchTrade(orderId!),
    enabled: !!orderId,
    refetchInterval: (q) => {
      const o = orderQuery.data;
      const t = q.state.data;
      if (o && isTerminal(o.status, t?.status ?? undefined)) return false;
      return 3000;
    },
  });

  const order = orderQuery.data;
  const trade = tradeQuery.data ?? null;
  const loading = orderQuery.isLoading;
  const polling = !order || !isTerminal(order.status, trade?.status);

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
          <OrderStatusTimeline order={order} trade={trade} polling={polling} />
        </CardContent>
      </Card>
    </div>
  );
}
