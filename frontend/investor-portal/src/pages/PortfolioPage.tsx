import { useQuery } from '@tanstack/react-query';
import { Loader2 } from 'lucide-react';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { formatUsd } from '@/lib/utils';
import { ConnectWalletButton } from '@/components/ConnectWalletButton';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

export function PortfolioPage() {
  const { user } = useAuth();

  const { data, isLoading, error } = useQuery({
    queryKey: ['portfolio', user?.id],
    queryFn: () => api.getPortfolio(user!.id),
    enabled: !!user?.id,
  });

  if (!user) return null;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Portfolio</h1>
          <p className="mt-1 text-muted-foreground">USDC balances and token holdings</p>
        </div>
        <ConnectWalletButton />
      </div>

      {isLoading && (
        <div className="flex items-center text-muted-foreground">
          <Loader2 className="mr-2 h-5 w-5 animate-spin" />
          Loading portfolio…
        </div>
      )}

      {error && (
        <div className="rounded-lg border border-destructive/30 bg-destructive/5 p-4 text-destructive">
          {error instanceof Error ? error.message : 'Failed to load portfolio'}
        </div>
      )}

      {data && (
        <div className="grid gap-4 md:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle>Fiat balance</CardTitle>
              <CardDescription>Custodial USDC in Payment Service</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              {data.fiatBalances.length === 0 ? (
                <p className="text-sm text-muted-foreground">No balance rows yet</p>
              ) : (
                data.fiatBalances.map((b) => (
                  <div key={b.currency} className="flex justify-between text-sm">
                    <span>{b.currency}</span>
                    <span>
                      {formatUsd(b.available)} available
                      {b.held > 0 && ` · ${formatUsd(b.held)} held`}
                    </span>
                  </div>
                ))
              )}
              {data.primaryWalletAddress && (
                <p className="font-mono text-xs text-muted-foreground">{data.primaryWalletAddress}</p>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Token holdings</CardTitle>
              <CardDescription>From Token Issuance registry</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              {data.tokenHoldings.length === 0 ? (
                <p className="text-sm text-muted-foreground">No token holdings yet — buy on a listing to start</p>
              ) : (
                data.tokenHoldings.map((h) => (
                  <div key={h.contractId} className="flex justify-between text-sm">
                    <span className="font-medium">{h.tokenSymbol}</span>
                    <span>{h.balance.toLocaleString()} tokens</span>
                  </div>
                ))
              )}
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}
