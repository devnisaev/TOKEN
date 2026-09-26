import { useQuery } from '@tanstack/react-query';
import { Loader2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { formatUsd } from '@/lib/utils';
import { ConnectWalletButton } from '@/components/ConnectWalletButton';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

export function PortfolioPage() {
  const { user } = useAuth();

  const { data, isLoading, error } = useQuery({
    queryKey: ['portfolio-bff', user?.id],
    queryFn: () => api.getPortfolioBff(user!.id),
    enabled: !!user?.id,
  });

  if (!user) return null;

  const balance = data?.balance;
  const recentDividends = data?.recentDividends ?? [];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Portfolio</h1>
          <p className="mt-1 text-muted-foreground">USDC balances, token holdings, and recent dividends</p>
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

      {balance && (
        <div className="grid gap-4 md:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle>Fiat balance</CardTitle>
              <CardDescription>Custodial USDC in Payment Service</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              {balance.fiatBalances.length === 0 ? (
                <p className="text-sm text-muted-foreground">No balance rows yet</p>
              ) : (
                balance.fiatBalances.map((b) => (
                  <div key={b.currency} className="flex justify-between text-sm">
                    <span>{b.currency}</span>
                    <span>
                      {formatUsd(b.available)} available
                      {b.held > 0 && ` · ${formatUsd(b.held)} held`}
                    </span>
                  </div>
                ))
              )}
              {balance.primaryWalletAddress && (
                <p className="font-mono text-xs text-muted-foreground">{balance.primaryWalletAddress}</p>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Token holdings</CardTitle>
              <CardDescription>From Token Issuance registry</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              {balance.tokenHoldings.length === 0 ? (
                <p className="text-sm text-muted-foreground">No token holdings yet — buy on a listing to start</p>
              ) : (
                balance.tokenHoldings.map((h) => (
                  <div key={h.contractId} className="flex items-center justify-between text-sm">
                    <span className="font-medium">{h.tokenSymbol}</span>
                    <div className="flex items-center gap-3">
                      <span>{h.balance.toLocaleString()} tokens</span>
                      {h.flatId && h.balance > 0 && (
                        <Link
                          to={`/portfolio/sell/${h.contractId}`}
                          className="text-xs text-primary underline-offset-4 hover:underline"
                        >
                          Sell
                        </Link>
                      )}
                    </div>
                  </div>
                ))
              )}
            </CardContent>
          </Card>
        </div>
      )}

      {recentDividends.length > 0 && (
        <Card>
          <CardHeader className="flex flex-row items-center justify-between">
            <div>
              <CardTitle>Recent dividends</CardTitle>
              <CardDescription>Latest rental income distributions</CardDescription>
            </div>
            <Link to="/dividends" className="text-sm text-primary underline-offset-4 hover:underline">
              View all
            </Link>
          </CardHeader>
          <CardContent className="space-y-2">
            {recentDividends.slice(0, 5).map((d) => (
              <div key={d.id} className="flex justify-between text-sm">
                <span className="text-muted-foreground">
                  {d.periodEnd ?? d.paidAt?.slice(0, 10) ?? 'Period'}
                </span>
                <span>
                  {formatUsd(d.amountUsd)} · {d.status}
                </span>
              </div>
            ))}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
