import { FormEvent, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Loader2 } from 'lucide-react';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { formatUsd } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

export function LendingPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [borrowAmount, setBorrowAmount] = useState('10000');

  const dashboardQuery = useQuery({
    queryKey: ['lending-dashboard', user?.id],
    queryFn: () => api.getLendingDashboard(user!.id),
    enabled: !!user?.id,
  });

  const borrowMutation = useMutation({
    mutationFn: () =>
      api.borrowAgainstCollateral({
        investorId: user!.id,
        collateralPositionId: dashboardQuery.data!.collateral[0].id,
        borrowAmountUsd: Number(borrowAmount),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['lending-dashboard', user?.id] });
    },
  });

  function onBorrow(e: FormEvent) {
    e.preventDefault();
    if (!dashboardQuery.data?.collateral.length) return;
    borrowMutation.mutate();
  }

  const dashboard = dashboardQuery.data;

  return (
    <div className="mx-auto max-w-3xl space-y-6 p-4">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Collateral lending</h1>
        <p className="text-muted-foreground">Borrow USDC against staked property tokens</p>
      </div>

      {dashboard && (
        <div className="grid gap-4 sm:grid-cols-3 text-sm">
          <Card>
            <CardContent className="pt-4">
              <p className="text-muted-foreground">Collateral</p>
              <p className="text-lg font-semibold">{formatUsd(dashboard.totalCollateralUsd)}</p>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-4">
              <p className="text-muted-foreground">Outstanding</p>
              <p className="text-lg font-semibold">{formatUsd(dashboard.totalOutstandingUsd)}</p>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-4">
              <p className="text-muted-foreground">Available to borrow</p>
              <p className="text-lg font-semibold">{formatUsd(dashboard.availableBorrowUsd)}</p>
            </CardContent>
          </Card>
        </div>
      )}

      {dashboard?.collateral.length ? (
        <Card>
          <CardHeader>
            <CardTitle>Borrow</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={onBorrow} className="flex flex-wrap items-end gap-4">
              <div>
                <Label htmlFor="borrowAmount">Amount (USD)</Label>
                <Input
                  id="borrowAmount"
                  type="number"
                  min="1"
                  value={borrowAmount}
                  onChange={(e) => setBorrowAmount(e.target.value)}
                />
              </div>
              <Button type="submit" disabled={borrowMutation.isPending}>
                {borrowMutation.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                Borrow
              </Button>
            </form>
          </CardContent>
        </Card>
      ) : (
        <p className="text-muted-foreground">Deposit collateral via the API to enable borrowing.</p>
      )}
    </div>
  );
}
