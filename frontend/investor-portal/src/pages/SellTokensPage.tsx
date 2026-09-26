import { FormEvent, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery } from '@tanstack/react-query';
import { ArrowLeft, Loader2 } from 'lucide-react';
import { useAccount } from 'wagmi';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { formatUsd } from '@/lib/utils';
import { ConnectWalletButton } from '@/components/ConnectWalletButton';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

export function SellTokensPage() {
  const { contractId } = useParams<{ contractId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { address } = useAccount();
  const [tokenAmount, setTokenAmount] = useState('');
  const [priceUsd, setPriceUsd] = useState('');

  const portfolioQuery = useQuery({
    queryKey: ['portfolio-bff', user?.id],
    queryFn: () => api.getPortfolioBff(user!.id),
    enabled: !!user?.id,
  });

  const holding = portfolioQuery.data?.balance.tokenHoldings.find((h) => h.contractId === contractId);

  const sellMutation = useMutation({
    mutationFn: () => {
      const wallet = address ?? user?.walletAddress;
      if (!user || !wallet || !holding?.flatId) {
        throw new Error('Connect wallet and ensure holding has flat metadata');
      }
      const amount = parseInt(tokenAmount, 10);
      const price = parseFloat(priceUsd);
      if (!amount || amount < 1) {
        throw new Error('Enter a valid token amount');
      }
      if (!price || price <= 0) {
        throw new Error('Enter a valid price per token');
      }
      return api.placeSellOrder({
        flatId: holding.flatId,
        contractId: holding.contractId,
        sellerInvestorId: user.id,
        sellerWallet: wallet,
        priceUsd: price,
        tokenAmount: amount,
        title: `Sell ${holding.tokenSymbol}`,
      });
    },
    onSuccess: (order) => navigate(`/orders/${order.id}`),
  });

  function onSell(e: FormEvent) {
    e.preventDefault();
    sellMutation.mutate();
  }

  if (portfolioQuery.isLoading) {
    return (
      <div className="flex items-center justify-center py-20 text-muted-foreground">
        <Loader2 className="mr-2 h-5 w-5 animate-spin" />
        Loading holding…
      </div>
    );
  }

  if (!holding) {
    return (
      <div className="space-y-4">
        <Link to="/portfolio" className="inline-flex items-center text-sm text-muted-foreground">
          <ArrowLeft className="mr-1 h-4 w-4" /> Portfolio
        </Link>
        <p className="text-destructive">Holding not found</p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-lg space-y-6">
      <Link to="/portfolio" className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="mr-1 h-4 w-4" /> Portfolio
      </Link>

      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Sell {holding.tokenSymbol}</h1>
          <p className="text-sm text-muted-foreground">
            {holding.balance.toLocaleString()} tokens available
            {holding.tokenPriceUsd != null && ` · ref ${formatUsd(holding.tokenPriceUsd)}/token`}
          </p>
        </div>
        <ConnectWalletButton />
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Secondary sell order</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={onSell} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="tokenAmount">Tokens to sell</Label>
              <Input
                id="tokenAmount"
                type="number"
                min={1}
                max={holding.balance}
                value={tokenAmount}
                onChange={(e) => setTokenAmount(e.target.value)}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="priceUsd">Price per token (USD)</Label>
              <Input
                id="priceUsd"
                type="number"
                min={0.01}
                step="0.01"
                value={priceUsd}
                onChange={(e) => setPriceUsd(e.target.value)}
                placeholder={holding.tokenPriceUsd?.toString() ?? '1.00'}
                required
              />
            </div>
            {sellMutation.error && (
              <p className="text-sm text-destructive">
                {sellMutation.error instanceof Error ? sellMutation.error.message : 'Sell failed'}
              </p>
            )}
            <Button type="submit" className="w-full" disabled={sellMutation.isPending}>
              {sellMutation.isPending ? 'Placing sell order…' : 'Place sell order'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
