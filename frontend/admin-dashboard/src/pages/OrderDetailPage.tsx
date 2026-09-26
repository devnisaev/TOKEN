import { FormEvent, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, Loader2 } from 'lucide-react';
import { api } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

async function fetchTrade(orderId: string) {
  try {
    return await api.getOrderTrade(orderId);
  } catch {
    return null;
  }
}

export function OrderDetailPage() {
  const { orderId } = useParams<{ orderId: string }>();
  const queryClient = useQueryClient();
  const [paymentId, setPaymentId] = useState('');
  const [transferId, setTransferId] = useState('');

  const orderQuery = useQuery({
    queryKey: ['admin-order', orderId],
    queryFn: () => api.getOrder(orderId!),
    enabled: !!orderId,
  });

  const tradeQuery = useQuery({
    queryKey: ['admin-order-trade', orderId],
    queryFn: () => fetchTrade(orderId!),
    enabled: !!orderId,
  });

  const settleMutation = useMutation({
    mutationFn: () =>
      api.settleOrder(orderId!, {
        paymentId,
        transferId,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-order', orderId] });
      queryClient.invalidateQueries({ queryKey: ['admin-order-trade', orderId] });
    },
  });

  const order = orderQuery.data;
  const trade = tradeQuery.data;
  const canSettle = order && order.status !== 'SETTLED' && trade && trade.status !== 'SETTLED';

  useEffect(() => {
    if (trade?.paymentId) {
      setPaymentId(trade.paymentId);
    }
    if (trade?.transferId) {
      setTransferId(trade.transferId);
    }
  }, [trade?.paymentId, trade?.transferId]);

  function onSettle(e: FormEvent) {
    e.preventDefault();
    settleMutation.mutate();
  }

  if (orderQuery.isLoading) {
    return (
      <div className="flex items-center text-muted-foreground">
        <Loader2 className="mr-2 h-5 w-5 animate-spin" />
        Loading order…
      </div>
    );
  }

  if (orderQuery.error || !order) {
    return (
      <div className="space-y-4">
        <Link to="/orders" className="inline-flex items-center text-sm text-muted-foreground">
          <ArrowLeft className="mr-1 h-4 w-4" /> Orders
        </Link>
        <p className="text-destructive">Order not found</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <Link to="/orders" className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="mr-1 h-4 w-4" /> Orders
      </Link>

      <div>
        <h1 className="text-3xl font-bold tracking-tight">Order {order.id.slice(0, 8)}…</h1>
        <p className="text-muted-foreground">Marketplace order and trade drill-down</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Order</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-3 text-sm md:grid-cols-2">
          <div>
            <span className="text-muted-foreground">Status</span>
            <p className="font-medium">{order.status}</p>
          </div>
          <div>
            <span className="text-muted-foreground">Listing</span>
            <p className="font-mono text-xs">{order.listingId}</p>
          </div>
          <div>
            <span className="text-muted-foreground">Buyer</span>
            <p className="font-mono text-xs">{order.buyerId}</p>
          </div>
          <div>
            <span className="text-muted-foreground">Tokens / total</span>
            <p className="font-medium">
              {order.tokenAmount.toLocaleString()} · ${order.totalPriceUsd}
            </p>
          </div>
          {order.buyerWallet && (
            <div className="md:col-span-2">
              <span className="text-muted-foreground">Buyer wallet</span>
              <p className="font-mono text-xs break-all">{order.buyerWallet}</p>
            </div>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Trade</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {!trade ? (
            <p className="text-sm text-muted-foreground">No trade record yet.</p>
          ) : (
            <div className="grid gap-3 text-sm md:grid-cols-2">
              <div>
                <span className="text-muted-foreground">Trade status</span>
                <p className="font-medium">{trade.status}</p>
              </div>
              <div>
                <span className="text-muted-foreground">Trade ID</span>
                <p className="font-mono text-xs">{trade.id}</p>
              </div>
              {trade.paymentId && (
                <div>
                  <span className="text-muted-foreground">Payment ID</span>
                  <p className="font-mono text-xs">{trade.paymentId}</p>
                </div>
              )}
              {trade.transferId && (
                <div>
                  <span className="text-muted-foreground">Transfer ID</span>
                  <p className="font-mono text-xs">{trade.transferId}</p>
                </div>
              )}
            </div>
          )}

          {canSettle && (
            <form onSubmit={onSettle} className="space-y-3 border-t pt-4">
              <p className="text-sm text-muted-foreground">
                Mark settled after payment confirmed and on-chain transfer completed.
              </p>
              <div className="grid gap-3 md:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="paymentId">Payment ID</Label>
                  <Input
                    id="paymentId"
                    value={paymentId}
                    onChange={(e) => setPaymentId(e.target.value)}
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="transferId">Transfer ID</Label>
                  <Input
                    id="transferId"
                    value={transferId}
                    onChange={(e) => setTransferId(e.target.value)}
                    required
                  />
                </div>
              </div>
              {settleMutation.error && (
                <p className="text-sm text-destructive">
                  {settleMutation.error instanceof Error
                    ? settleMutation.error.message
                    : 'Settle failed'}
                </p>
              )}
              {settleMutation.isSuccess && (
                <p className="text-sm text-green-600">Order marked settled.</p>
              )}
              <Button type="submit" disabled={settleMutation.isPending}>
                {settleMutation.isPending ? 'Settling…' : 'Mark settled'}
              </Button>
            </form>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
