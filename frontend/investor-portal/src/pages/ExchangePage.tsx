import { FormEvent, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, Loader2 } from 'lucide-react';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { formatUsd } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

export function ExchangePage() {
  const { contractId } = useParams<{ contractId: string }>();
  const [searchParams] = useSearchParams();
  const flatId = searchParams.get('flatId') ?? '';
  const { user } = useAuth();
  const queryClient = useQueryClient();

  const [side, setSide] = useState<'BID' | 'ASK'>('BID');
  const [price, setPrice] = useState('100.00');
  const [quantity, setQuantity] = useState('1');

  const flatQuery = useQuery({
    queryKey: ['flat-bff', flatId],
    queryFn: () => api.getFlatDetail(flatId),
    enabled: !!flatId,
  });

  const bookQuery = useQuery({
    queryKey: ['exchange-book', contractId],
    queryFn: () => api.getExchangeBook(contractId!),
    enabled: !!contractId,
    refetchInterval: 5000,
  });

  const tickerQuery = useQuery({
    queryKey: ['exchange-ticker', contractId, flatId],
    queryFn: () => api.getExchangeTicker(contractId!, flatId),
    enabled: !!contractId && !!flatId,
    refetchInterval: 10000,
  });

  const ordersQuery = useQuery({
    queryKey: ['exchange-orders', user?.id],
    queryFn: () => api.listExchangeOrders(user!.id),
    enabled: !!user?.id,
  });

  const placeMutation = useMutation({
    mutationFn: () =>
      api.placeExchangeOrder({
        contractId: contractId!,
        flatId,
        buildingId: flatQuery.data!.buildingId,
        side,
        limitPriceUsd: Number(price),
        quantity: Number(quantity),
        investorId: user!.id,
        walletAddress: user!.walletAddress ?? '0x0000000000000000000000000000000000000001',
        liquidityTier: 'TIER_1',
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['exchange-book', contractId] });
      queryClient.invalidateQueries({ queryKey: ['exchange-orders', user?.id] });
      queryClient.invalidateQueries({ queryKey: ['exchange-ticker', contractId, flatId] });
    },
  });

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!flatQuery.data?.buildingId) return;
    placeMutation.mutate();
  }

  if (!contractId || !flatId) {
    return <p className="text-destructive">Missing contract or flat id.</p>;
  }

  return (
    <div className="space-y-6">
      <Link to="/portfolio" className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="mr-1 h-4 w-4" />
        Back to portfolio
      </Link>

      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Exchange</h1>
          <p className="text-muted-foreground">
            CLOB · {flatQuery.data?.buildingName ?? contractId.slice(0, 8)}
          </p>
        </div>
        <Link to={`/exchange/${contractId}/swap?flatId=${flatId}`}>
          <Button variant="outline" size="sm">AMM swap</Button>
        </Link>
      </div>

      {tickerQuery.data && (
        <div className="grid gap-4 sm:grid-cols-4 text-sm">
          <Card>
            <CardContent className="pt-4">
              <p className="text-muted-foreground">Last</p>
              <p className="text-lg font-semibold">
                {tickerQuery.data.lastPriceUsd != null ? formatUsd(tickerQuery.data.lastPriceUsd) : '—'}
              </p>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-4">
              <p className="text-muted-foreground">NAV / token</p>
              <p className="text-lg font-semibold">
                {tickerQuery.data.navPerTokenUsd != null ? formatUsd(tickerQuery.data.navPerTokenUsd) : '—'}
              </p>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-4">
              <p className="text-muted-foreground">vs NAV</p>
              <p className="text-lg font-semibold">
                {tickerQuery.data.navDeltaPct != null ? `${tickerQuery.data.navDeltaPct.toFixed(2)}%` : '—'}
              </p>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-4">
              <p className="text-muted-foreground">24h volume</p>
              <p className="text-lg font-semibold">{tickerQuery.data.volume24hTokens} tokens</p>
            </CardContent>
          </Card>
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Order book</CardTitle>
          </CardHeader>
          <CardContent>
            {bookQuery.isLoading && (
              <div className="flex items-center text-muted-foreground">
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Loading depth…
              </div>
            )}
            {bookQuery.data && (
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <p className="mb-2 font-medium text-emerald-600">Bids</p>
                  {bookQuery.data.bids.length === 0 ? (
                    <p className="text-muted-foreground">No bids</p>
                  ) : (
                    bookQuery.data.bids.map((l) => (
                      <div key={l.priceUsd} className="flex justify-between py-0.5">
                        <span>{formatUsd(l.priceUsd)}</span>
                        <span>{l.totalQuantity}</span>
                      </div>
                    ))
                  )}
                </div>
                <div>
                  <p className="mb-2 font-medium text-red-600">Asks</p>
                  {bookQuery.data.asks.length === 0 ? (
                    <p className="text-muted-foreground">No asks</p>
                  ) : (
                    bookQuery.data.asks.map((l) => (
                      <div key={l.priceUsd} className="flex justify-between py-0.5">
                        <span>{formatUsd(l.priceUsd)}</span>
                        <span>{l.totalQuantity}</span>
                      </div>
                    ))
                  )}
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Place limit order</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={onSubmit} className="space-y-4">
              <div className="flex gap-2">
                <Button type="button" variant={side === 'BID' ? 'default' : 'outline'} onClick={() => setSide('BID')}>
                  Bid
                </Button>
                <Button type="button" variant={side === 'ASK' ? 'default' : 'outline'} onClick={() => setSide('ASK')}>
                  Ask
                </Button>
              </div>
              <div className="space-y-2">
                <Label htmlFor="price">Limit price (USD / token)</Label>
                <Input id="price" type="number" step="0.01" required value={price} onChange={(e) => setPrice(e.target.value)} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="qty">Quantity (tokens)</Label>
                <Input id="qty" type="number" min={1} required value={quantity} onChange={(e) => setQuantity(e.target.value)} />
              </div>
              {placeMutation.error && (
                <p className="text-sm text-destructive">
                  {placeMutation.error instanceof Error ? placeMutation.error.message : 'Order failed'}
                </p>
              )}
              <Button type="submit" disabled={placeMutation.isPending || flatQuery.isLoading} className="w-full">
                {placeMutation.isPending ? 'Placing…' : `Place ${side}`}
              </Button>
            </form>
          </CardContent>
        </Card>
      </div>

      {ordersQuery.data && ordersQuery.data.content.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Your open orders</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2 text-sm">
            {ordersQuery.data.content.map((o) => (
              <div key={o.id} className="flex items-center justify-between border-b py-2 last:border-0">
                <span>
                  {o.side} · {formatUsd(o.limitPriceUsd)} · {o.remainingQuantity} left
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() =>
                    api.cancelExchangeOrder(o.id, user!.id).then(() =>
                      queryClient.invalidateQueries({ queryKey: ['exchange-orders', user?.id] }),
                    )
                  }
                >
                  Cancel
                </Button>
              </div>
            ))}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
