import { useQuery } from '@tanstack/react-query';
import { Loader2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { formatUsd } from '@/lib/utils';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

export function DividendsPage() {
  const { user } = useAuth();

  const { data, isLoading, error } = useQuery({
    queryKey: ['dividends', user?.id],
    queryFn: () => api.listDividends(user!.id),
    enabled: !!user?.id,
  });

  if (!user) return null;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Dividends</h1>
        <p className="mt-1 text-muted-foreground">Rental income distributions to your token holdings</p>
      </div>

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

      {data && data.length === 0 && (
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
            <CardTitle>Payment history</CardTitle>
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
