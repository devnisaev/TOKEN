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

export function PoolSwapPage() {
  const { contractId } = useParams<{ contractId: string }>();
  const [searchParams] = useSearchParams();
  const flatId = searchParams.get('flatId') ?? '';
  const { user } = useAuth();
  const queryClient = useQueryClient();

  const [direction, setDirection] = useState<'USDC_TO_TOKEN' | 'TOKEN_TO_USDC'>('USDC_TO_TOKEN');
  const [amountIn, setAmountIn] = useState('1000.00');

  const poolQuery = useQuery({
    queryKey: ['pool-by-contract', contractId],
    queryFn: () => api.getPoolByContract(contractId!),
    enabled: !!contractId,
    refetchInterval: 8000,
  });

  const quoteQuery = useQuery({
    queryKey: ['pool-quote', poolQuery.data?.id, direction, amountIn],
    queryFn: () =>
      api.quotePoolSwap(poolQuery.data!.id, {
        direction,
        amountIn: Number(amountIn),
      }),
    enabled: !!poolQuery.data?.id && Number(amountIn) > 0,
  });

  const swapMutation = useMutation({
    mutationFn: () =>
      api.executePoolSwap(poolQuery.data!.id, {
        direction,
        amountIn: Number(amountIn),
        investorId: user!.id,
        walletAddress: user!.walletAddress ?? '0x0000000000000000000000000000000000000001',
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pool-by-contract', contractId] });
      queryClient.invalidateQueries({ queryKey: ['pool-quote', poolQuery.data?.id] });
    },
  });

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    swapMutation.mutate();
  }

  if (!contractId) {
    return <p className="text-destructive">Missing contract id.</p>;
  }

  const pool = poolQuery.data;
  const quote = quoteQuery.data;

  return (
    <div className="mx-auto max-w-2xl space-y-6 p-4">
      <Link
        to={`/exchange/${contractId}?flatId=${flatId}`}
        className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="h-4 w-4" />
        Back to order book
      </Link>

      <Card>
        <CardHeader>
          <CardTitle>AMM pool swap</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {poolQuery.isLoading && <p className="text-muted-foreground">Loading pool…</p>}
          {poolQuery.isError && (
            <p className="text-destructive">No liquidity pool for this token yet.</p>
          )}
          {pool && (
            <>
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <p className="text-muted-foreground">Spot price</p>
                  <p className="font-medium">{formatUsd(pool.spotPriceUsd)} / token</p>
                </div>
                <div>
                  <p className="text-muted-foreground">Status</p>
                  <p className="font-medium">{pool.status}</p>
                </div>
                <div>
                  <p className="text-muted-foreground">Token reserve</p>
                  <p className="font-medium">{pool.tokenReserve.toLocaleString()}</p>
                </div>
                <div>
                  <p className="text-muted-foreground">USDC reserve</p>
                  <p className="font-medium">{formatUsd(pool.usdcReserve)}</p>
                </div>
              </div>

              {pool.status !== 'ACTIVE' && (
                <p className="rounded-md bg-amber-50 p-3 text-sm text-amber-900">
                  Pool is {pool.status.toLowerCase()} — swaps are disabled (NAV circuit breaker may have triggered).
                </p>
              )}

              <form onSubmit={onSubmit} className="space-y-4">
                <div className="flex gap-2">
                  <Button
                    type="button"
                    variant={direction === 'USDC_TO_TOKEN' ? 'default' : 'outline'}
                    onClick={() => setDirection('USDC_TO_TOKEN')}
                  >
                    Buy tokens
                  </Button>
                  <Button
                    type="button"
                    variant={direction === 'TOKEN_TO_USDC' ? 'default' : 'outline'}
                    onClick={() => setDirection('TOKEN_TO_USDC')}
                  >
                    Sell tokens
                  </Button>
                </div>

                <div>
                  <Label htmlFor="amountIn">
                    {direction === 'USDC_TO_TOKEN' ? 'USDC amount' : 'Token amount'}
                  </Label>
                  <Input
                    id="amountIn"
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={amountIn}
                    onChange={(e) => setAmountIn(e.target.value)}
                  />
                </div>

                {quote && (
                  <div className="rounded-md border p-3 text-sm">
                    <p>You receive: {quote.amountOut.toLocaleString()} {direction === 'USDC_TO_TOKEN' ? 'tokens' : 'USDC'}</p>
                    <p className="text-muted-foreground">Fee: {formatUsd(quote.feeUsd)}</p>
                  </div>
                )}

                <Button
                  type="submit"
                  disabled={pool.status !== 'ACTIVE' || swapMutation.isPending || !quote}
                  className="w-full"
                >
                  {swapMutation.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                  Execute swap
                </Button>
              </form>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
