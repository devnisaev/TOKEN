import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Loader2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { formatUsd } from '@/lib/utils';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

export function DividendsPage() {
  const { user } = useAuth();

  const dividendsQuery = useQuery({
    queryKey: ['dividends', user?.id],
    queryFn: () => api.listDividends(user!.id),
    enabled: !!user?.id,
  });

  const payoutsQuery = useQuery({
    queryKey: ['payouts', user?.id],
    queryFn: () => api.listPayouts(user!.id),
    enabled: !!user?.id,
  });

  const dividendPayouts = useMemo(
    () => payoutsQuery.data?.content.filter((p) => p.purpose === 'DIVIDEND') ?? [],
    [payoutsQuery.data],
  );

  const ytdWithholding = useMemo(
    () =>
      dividendPayouts.reduce(
        (sum, p) => sum + (p.withholdingAmountUsd ?? 0),
        0,
      ),
    [dividendPayouts],
  );

  if (!user) return null;

  const isLoading = dividendsQuery.isLoading || payoutsQuery.isLoading;
  const error = dividendsQuery.error ?? payoutsQuery.error;
  const data = dividendsQuery.data;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Dividends</h1>
        <p className="mt-1 text-muted-foreground">Rental income distributions and tax withholding</p>
      </div>

      {dividendPayouts.length > 0 && (
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">YTD withholding</CardTitle>
          </CardHeader>
          <CardContent className="text-2xl font-bold">{formatUsd(ytdWithholding)}</CardContent>
        </Card>
      )}

      {isLoading && (
        <div className="flex items-center text-muted-foreground">
          <Loader2 className="mr-2 h-5 w-5 animate-spin" />
          Loading dividend history…
        </div>
      )}

      {error && (
        <div className="rounded-lg border border-destructive/30 bg-destructive/5 p-4 text-destructive">
          {error instanceof Error ? error.message : 'Failed to load dividends'}
        </div>
      )}

      {dividendPayouts.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Payout breakdown</CardTitle>
            <CardDescription>Gross, withholding, and net amounts from Payment Service</CardDescription>
          </CardHeader>
          <CardContent className="overflow-x-auto p-0">
            <table className="w-full text-sm">
              <thead className="border-b bg-muted/50">
                <tr>
                  <th className="px-4 py-3 text-left font-medium">Period</th>
                  <th className="px-4 py-3 text-left font-medium">Gross</th>
                  <th className="px-4 py-3 text-left font-medium">Withholding</th>
                  <th className="px-4 py-3 text-left font-medium">Net</th>
                  <th className="px-4 py-3 text-left font-medium">Status</th>
                </tr>
              </thead>
              <tbody>
                {dividendPayouts.map((p) => (
                  <tr key={p.id} className="border-b last:border-0">
                    <td className="px-4 py-3">{p.period ?? '—'}</td>
                    <td className="px-4 py-3">{formatUsd(p.grossAmountUsd ?? p.amount)}</td>
                    <td className="px-4 py-3">{formatUsd(p.withholdingAmountUsd ?? 0)}</td>
                    <td className="px-4 py-3">{formatUsd(p.amount)}</td>
                    <td className="px-4 py-3">
                      <span className="rounded-md bg-secondary px-2 py-0.5 text-xs">{p.status}</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </CardContent>
        </Card>
      )}

      {data && data.length === 0 && dividendPayouts.length === 0 && !isLoading && (
        <Card>
          <CardContent className="py-8 text-center text-muted-foreground">
            No dividends yet. Hold tokens in a rented property to receive pro-rata payouts.
            <div className="mt-4">
              <Link to="/" className="text-primary underline-offset-4 hover:underline">
                Browse listings
              </Link>
            </div>
          </CardContent>
        </Card>
      )}

      {data && data.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Issuance records</CardTitle>
            <CardDescription>{data.length} distribution(s)</CardDescription>
          </CardHeader>
          <CardContent className="overflow-x-auto p-0">
            <table className="w-full text-sm">
              <thead className="border-b bg-muted/50">
                <tr>
                  <th className="px-4 py-3 text-left font-medium">Period</th>
                  <th className="px-4 py-3 text-left font-medium">Tokens held</th>
                  <th className="px-4 py-3 text-left font-medium">Amount</th>
                  <th className="px-4 py-3 text-left font-medium">Status</th>
                  <th className="px-4 py-3 text-left font-medium">Paid</th>
                </tr>
              </thead>
              <tbody>
                {data.map((d) => (
                  <tr key={d.id} className="border-b last:border-0">
                    <td className="px-4 py-3">
                      {d.periodStart && d.periodEnd ? `${d.periodStart} → ${d.periodEnd}` : '—'}
                    </td>
                    <td className="px-4 py-3">{d.tokensHeld?.toLocaleString() ?? '—'}</td>
                    <td className="px-4 py-3">{formatUsd(d.amountUsd)}</td>
                    <td className="px-4 py-3">
                      <span className="rounded-md bg-secondary px-2 py-0.5 text-xs">{d.status}</span>
                    </td>
                    <td className="px-4 py-3 text-xs text-muted-foreground">
                      {d.paidAt ? new Date(d.paidAt).toLocaleDateString() : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
