import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Loader2 } from 'lucide-react';
import { EmptyState, StatusBadge } from '@tokenrealty/shared-ui';
import { api } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export function MaintenanceTicketsPage() {
  const queryClient = useQueryClient();

  const { data, isLoading, error } = useQuery({
    queryKey: ['maintenance-tickets'],
    queryFn: () => api.listMaintenanceTickets(),
  });

  const resolveMutation = useMutation({
    mutationFn: (id: string) => api.updateMaintenanceTicket(id, { status: 'RESOLVED' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['maintenance-tickets'] }),
  });

  const tickets = data ?? [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Maintenance tickets</h1>
        <p className="text-muted-foreground">Review and resolve tenant maintenance requests</p>
      </div>

      {isLoading && (
        <div className="flex items-center text-muted-foreground">
          <Loader2 className="mr-2 h-5 w-5 animate-spin" />
          Loading…
        </div>
      )}

      {error && (
        <p className="text-destructive">
          {error instanceof Error ? error.message : 'Failed to load tickets'}
        </p>
      )}

      {!isLoading && tickets.length === 0 && (
        <EmptyState title="No tickets" description="Tenant maintenance requests will appear here." />
      )}

      <ul className="space-y-3">
        {tickets.map((ticket) => (
          <li key={ticket.id}>
            <Card>
              <CardHeader className="flex flex-row items-start justify-between space-y-0">
                <div>
                  <CardTitle className="text-base">{ticket.title}</CardTitle>
                  <p className="text-xs text-muted-foreground">
                    Unit {(ticket.flatId ?? '—').slice(0, 8)}… · tenant{' '}
                    {(ticket.tenantId ?? '—').slice(0, 8)}…
                  </p>
                </div>
                <StatusBadge status={ticket.status ?? 'OPEN'} />
              </CardHeader>
              <CardContent className="flex items-start justify-between gap-4">
                <p className="text-sm text-muted-foreground">{ticket.description}</p>
                {ticket.status === 'OPEN' && (
                  <Button
                    size="sm"
                    disabled={resolveMutation.isPending}
                    onClick={() => resolveMutation.mutate(ticket.id)}
                  >
                    Mark resolved
                  </Button>
                )}
              </CardContent>
            </Card>
          </li>
        ))}
      </ul>
    </div>
  );
}
