import { FormEvent, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Loader2, Wrench } from 'lucide-react';
import { EmptyState, StatusBadge } from '@tokenrealty/shared-ui';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

export function MaintenancePage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [selectedLeaseId, setSelectedLeaseId] = useState<string | null>(null);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');

  const maintenanceQuery = useQuery({
    queryKey: ['tenant-maintenance-bff', user?.id],
    queryFn: () => api.listTenantMaintenanceBff(user!.id),
    enabled: !!user?.id,
  });

  const items = maintenanceQuery.data ?? [];
  const item =
    items.find((i) => i.lease?.id && i.lease.id === selectedLeaseId) ?? items[0] ?? null;
  const lease = item?.lease?.id ? item.lease : null;
  const flat = item?.flat ?? null;
  const openTickets = item?.openTickets ?? [];

  const ticketsQuery = useQuery({
    queryKey: ['maintenance-tickets', user?.id],
    queryFn: () => api.listMaintenanceTickets(user!.id),
    enabled: !!user?.id,
  });

  const leaseId = lease?.id;

  const createMutation = useMutation({
    mutationFn: () =>
      api.createMaintenanceTicket({
        leaseId: leaseId!,
        title,
        description,
      }),
    onSuccess: () => {
      setTitle('');
      setDescription('');
      queryClient.invalidateQueries({ queryKey: ['tenant-maintenance-bff', user?.id] });
      queryClient.invalidateQueries({ queryKey: ['maintenance-tickets', user?.id] });
    },
  });

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    createMutation.mutate();
  }

  if (maintenanceQuery.isLoading) {
    return (
      <div className="flex items-center justify-center py-20 text-muted-foreground">
        <Loader2 className="mr-2 h-5 w-5 animate-spin" />
        Loading maintenance…
      </div>
    );
  }

  if (maintenanceQuery.error) {
    return (
      <p className="text-destructive">
        {maintenanceQuery.error instanceof Error
          ? maintenanceQuery.error.message
          : 'Failed to load maintenance'}
      </p>
    );
  }

  if (!lease || !leaseId) {
    return (
      <EmptyState
        title="No active lease"
        description="You need an active lease to submit maintenance requests."
      />
    );
  }

  const allTickets = ticketsQuery.data ?? [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Maintenance</h1>
        <p className="text-muted-foreground">Report issues with your rental unit</p>
      </div>

      {items.length > 1 && (
        <div className="flex flex-wrap gap-2">
          {items.map((entry) => (
            <Button
              key={entry.lease?.id ?? entry.flat?.flatId}
              size="sm"
              variant={entry.lease?.id === lease.id ? 'default' : 'outline'}
              onClick={() => setSelectedLeaseId(entry.lease?.id ?? null)}
            >
              {entry.flat?.flatNumber} · {entry.flat?.buildingName}
            </Button>
          ))}
        </div>
      )}

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-lg">
            <Wrench className="h-5 w-5" />
            {flat ? `${flat.buildingName} · Unit ${flat.flatNumber}` : 'Your unit'}
          </CardTitle>
          <p className="text-sm text-muted-foreground">
            {openTickets.length} open ticket{openTickets.length === 1 ? '' : 's'}
          </p>
        </CardHeader>
        <CardContent>
          <form onSubmit={onSubmit} className="space-y-3">
            <div className="space-y-2">
              <Label htmlFor="title">Issue title</Label>
              <Input
                id="title"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                maxLength={200}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="description">Description</Label>
              <Input
                id="description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                maxLength={2000}
                required
              />
            </div>
            {createMutation.error && (
              <p className="text-sm text-destructive">
                {createMutation.error instanceof Error
                  ? createMutation.error.message
                  : 'Failed to submit ticket'}
              </p>
            )}
            <Button type="submit" disabled={createMutation.isPending}>
              {createMutation.isPending ? 'Submitting…' : 'Submit request'}
            </Button>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Your tickets</CardTitle>
        </CardHeader>
        <CardContent>
          {ticketsQuery.isLoading && (
            <p className="text-sm text-muted-foreground">Loading…</p>
          )}
          {!ticketsQuery.isLoading && allTickets.length === 0 && (
            <EmptyState
              title="No tickets yet"
              description="Submit a request above when something needs fixing."
              className="py-6"
            />
          )}
          <ul className="space-y-3">
            {allTickets.map((ticket) => (
              <li
                key={ticket.id}
                className="flex items-start justify-between gap-4 rounded-md border p-3 text-sm"
              >
                <div>
                  <p className="font-medium">{ticket.title}</p>
                  <p className="mt-1 text-muted-foreground">{ticket.description}</p>
                  {ticket.createdAt && (
                    <p className="mt-1 text-xs text-muted-foreground">
                      {ticket.createdAt.slice(0, 10)}
                    </p>
                  )}
                </div>
                <StatusBadge status={ticket.status ?? 'OPEN'} />
              </li>
            ))}
          </ul>
        </CardContent>
      </Card>
    </div>
  );
}
