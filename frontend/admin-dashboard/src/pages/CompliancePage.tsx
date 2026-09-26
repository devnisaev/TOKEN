import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Loader2 } from 'lucide-react';
import { api } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

function expiryOneYear(): string {
  const d = new Date();
  d.setFullYear(d.getFullYear() + 1);
  return d.toISOString();
}

export function CompliancePage() {
  const queryClient = useQueryClient();

  const { data, isLoading, error } = useQuery({
    queryKey: ['compliance'],
    queryFn: () => api.listCompliance(),
  });

  const verifyMutation = useMutation({
    mutationFn: (id: string) => api.verifyCompliance(id, expiryOneYear()),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['compliance'] }),
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">KYC compliance</h1>
        <p className="text-muted-foreground">Review and approve investor whitelist records</p>
      </div>

      {isLoading && (
        <div className="flex items-center text-muted-foreground">
          <Loader2 className="mr-2 h-5 w-5 animate-spin" />
          Loading…
        </div>
      )}

      {error && (
        <p className="text-destructive">{error instanceof Error ? error.message : 'Failed to load'}</p>
      )}

      <ul className="space-y-3">
        {data?.content.map((record) => (
          <li key={record.id}>
            <Card>
              <CardHeader className="flex flex-row items-start justify-between space-y-0">
                <div>
                  <CardTitle className="text-base">{record.fullName ?? record.investorId}</CardTitle>
                  <p className="font-mono text-xs text-muted-foreground">{record.walletAddress}</p>
                </div>
                <span className="rounded-md bg-secondary px-2 py-1 text-xs font-medium">{record.status}</span>
              </CardHeader>
              <CardContent className="flex items-center justify-between">
                <span className="text-sm text-muted-foreground">{record.countryCode ?? '—'}</span>
                {record.status === 'PENDING' && (
                  <Button
                    size="sm"
                    disabled={verifyMutation.isPending}
                    onClick={() => verifyMutation.mutate(record.id)}
                  >
                    Approve KYC
                  </Button>
                )}
              </CardContent>
            </Card>
          </li>
        ))}
      </ul>

      {data?.content.length === 0 && !isLoading && (
        <p className="text-muted-foreground">No compliance records found.</p>
      )}
    </div>
  );
}
