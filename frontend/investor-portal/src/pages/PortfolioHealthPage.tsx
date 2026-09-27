import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export function PortfolioHealthPage() {
  const { user } = useAuth();

  const portfolioQuery = useQuery({
    queryKey: ['portfolio-bff', user?.id],
    queryFn: () => api.getPortfolioBff(user!.id),
    enabled: !!user?.id,
  });

  const flatIds = portfolioQuery.data?.balance.tokenHoldings
    .map((holding) => holding.flatId)
    .filter((id): id is string => !!id) ?? [];

  const healthQuery = useQuery({
    queryKey: ['portfolio-health', flatIds],
    queryFn: () => api.getPortfolioHealth(flatIds),
    enabled: flatIds.length > 0,
  });

  if (!user) return null;

  return (
    <div className="mx-auto max-w-2xl space-y-6 p-4">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Portfolio health</h1>
        <p className="text-muted-foreground">Latest asset health scores for your holdings</p>
      </div>

      {portfolioQuery.isLoading && (
        <p className="text-muted-foreground">Loading holdings…</p>
      )}

      {flatIds.length === 0 && portfolioQuery.isSuccess && (
        <p className="text-muted-foreground">No token holdings with linked assets yet.</p>
      )}

      <div className="space-y-3">
        {healthQuery.data?.map((item) => {
          const holding = portfolioQuery.data?.balance.tokenHoldings.find(
            (h) => h.flatId === item.flatId,
          );
          return (
            <Card key={item.flatId}>
              <CardHeader className="pb-2">
                <CardTitle className="text-base">
                  {holding?.tokenSymbol ?? 'Asset'}{' '}
                  <span className="font-normal text-muted-foreground">
                    · {item.flatId.slice(0, 8)}…
                  </span>
                </CardTitle>
              </CardHeader>
              <CardContent className="flex flex-wrap items-center justify-between gap-4 text-sm">
                <div>
                  <p className="text-muted-foreground">Health score</p>
                  <p className="text-lg font-semibold">
                    {item.healthScore != null ? `${item.healthScore.toFixed(1)}%` : '—'}
                  </p>
                </div>
                <div>
                  <p className="text-muted-foreground">ESG / Occupancy</p>
                  <p className="font-medium">
                    {item.esgFactor ?? '—'} / {item.occupancyFactor ?? '—'}
                  </p>
                </div>
                <Link
                  to={`/assets/${item.flatId}/sustainability`}
                  className="text-primary underline-offset-4 hover:underline"
                >
                  Sustainability
                </Link>
              </CardContent>
            </Card>
          );
        })}
      </div>
    </div>
  );
}
