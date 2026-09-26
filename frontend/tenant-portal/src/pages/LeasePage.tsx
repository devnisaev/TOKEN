import { FormEvent, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, Loader2 } from 'lucide-react';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { formatUsd } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

function currentPeriod(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
}

export function LeasePage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [selectedLeaseId, setSelectedLeaseId] = useState<string | null>(null);
  const [period, setPeriod] = useState(currentPeriod());
  const [paidNotice, setPaidNotice] = useState<string | null>(null);

  const leasesQuery = useQuery({
    queryKey: ['leases', user?.id],
    queryFn: () => api.listLeases(user!.id),
    enabled: !!user?.id,
  });

  const leases = leasesQuery.data ?? [];
  const lease = leases.find((l) => l.id === selectedLeaseId) ?? leases[0] ?? null;

  const paymentsQuery = useQuery({
    queryKey: ['rent-payments', lease?.id],
    queryFn: () => api.listRentPayments(lease!.id),
    enabled: !!lease?.id,
  });

  const payMutation = useMutation({
    mutationFn: () =>
      api.payRent({
        leaseId: lease!.id,
        period,
        amount: lease!.monthlyRentUsd,
      }),
    onSuccess: (payment) => {
      setPaidNotice(`Rent for ${period} recorded (${payment.status}).`);
      queryClient.invalidateQueries({ queryKey: ['rent-payments', lease?.id] });
    },
  });

  function onPay(e: FormEvent) {
    e.preventDefault();
    setPaidNotice(null);
    payMutation.mutate();
  }

  if (leasesQuery.isLoading) {
    return (
      <div className="flex items-center justify-center py-20 text-muted-foreground">
        <Loader2 className="mr-2 h-5 w-5 animate-spin" />
        Loading lease…
      </div>
    );
  }

  if (leasesQuery.error) {
    return (
      <p className="text-destructive">
        {leasesQuery.error instanceof Error ? leasesQuery.error.message : 'Failed to load lease'}
      </p>
    );
  }

  if (!lease) {
    return (
      <Card>
        <CardContent className="py-10 text-center text-muted-foreground">
          No active lease found. Contact your property manager.
        </CardContent>
      </Card>
    );
  }

  const paidThisPeriod = paymentsQuery.data?.some((p) => p.period === period && p.status !== 'FAILED');

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">My lease</h1>
        <p className="text-muted-foreground">Pay monthly rent to your SPV wallet</p>
      </div>

      {leases.length > 1 && (
        <div className="flex flex-wrap gap-2">
          {leases.map((item) => (
            <Button
              key={item.id}
              size="sm"
              variant={item.id === lease.id ? 'default' : 'outline'}
              onClick={() => {
                setSelectedLeaseId(item.id);
                setPaidNotice(null);
              }}
            >
              Flat {item.flatId.slice(0, 8)}…
            </Button>
          ))}
        </div>
      )}

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">{formatUsd(lease.monthlyRentUsd)} / month</CardTitle>
          <p className="text-sm text-muted-foreground">
            Flat {lease.flatId} · {lease.startDate} → {lease.endDate ?? 'open-ended'} · {lease.status}
          </p>
        </CardHeader>
        <CardContent className="space-y-4">
          <dl className="grid gap-2 text-sm">
            <div className="flex justify-between gap-4">
              <dt className="text-muted-foreground">Tenant wallet</dt>
              <dd className="break-all font-mono text-xs">{lease.tenantWallet}</dd>
            </div>
            <div className="flex justify-between gap-4">
              <dt className="text-muted-foreground">SPV wallet</dt>
              <dd className="break-all font-mono text-xs">{lease.spvWallet}</dd>
            </div>
          </dl>

          {paidNotice && (
            <p className="flex items-center gap-2 rounded-md bg-green-50 p-3 text-sm text-green-800">
              <CheckCircle2 className="h-4 w-4" />
              {paidNotice}
            </p>
          )}

          {paidThisPeriod ? (
            <p className="rounded-md bg-accent p-3 text-sm">Rent for {period} is already paid.</p>
          ) : (
            <form onSubmit={onPay} className="space-y-3 border-t pt-4">
              <div className="space-y-2">
                <Label htmlFor="period">Period (YYYY-MM)</Label>
                <Input
                  id="period"
                  pattern="\d{4}-\d{2}"
                  value={period}
                  onChange={(e) => setPeriod(e.target.value)}
                  required
                />
              </div>
              {payMutation.error && (
                <p className="text-sm text-destructive">
                  {payMutation.error instanceof Error ? payMutation.error.message : 'Payment failed'}
                </p>
              )}
              <Button type="submit" className="w-full" disabled={payMutation.isPending}>
                {payMutation.isPending ? 'Processing…' : `Pay ${formatUsd(lease.monthlyRentUsd)}`}
              </Button>
            </form>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Payment history</CardTitle>
        </CardHeader>
        <CardContent>
          {paymentsQuery.isLoading && <p className="text-sm text-muted-foreground">Loading…</p>}
          {paymentsQuery.data?.length === 0 && (
            <p className="text-sm text-muted-foreground">No payments yet.</p>
          )}
          <ul className="space-y-2">
            {paymentsQuery.data?.map((payment) => (
              <li key={payment.id} className="flex justify-between text-sm">
                <span>{payment.period}</span>
                <span>
                  {formatUsd(payment.amount)} · {payment.status}
                  {payment.paidAt && ` · ${payment.paidAt.slice(0, 10)}`}
                </span>
              </li>
            ))}
          </ul>
        </CardContent>
      </Card>
    </div>
  );
}
