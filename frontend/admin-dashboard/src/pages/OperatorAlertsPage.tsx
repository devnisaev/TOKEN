import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export function OperatorAlertsPage() {
  const queryClient = useQueryClient();
  const alertsQuery = useQuery({
    queryKey: ['operator-alerts'],
    queryFn: () => api.listOperatorAlerts(),
  });

  const generateMutation = useMutation({
    mutationFn: () => api.generateOperatorAlerts(30),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['operator-alerts'] });
      queryClient.invalidateQueries({ queryKey: ['operator-kpis'] });
    },
  });

  const acknowledgeMutation = useMutation({
    mutationFn: (id: string) => api.acknowledgeOperatorAlert(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['operator-alerts'] });
      queryClient.invalidateQueries({ queryKey: ['operator-kpis'] });
    },
  });

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-6">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold">Operator alerts</h1>
          <p className="text-muted-foreground">
            Health, occupancy, and insurance expiry signals
          </p>
        </div>
        <Button
          onClick={() => generateMutation.mutate()}
          disabled={generateMutation.isPending}
        >
          {generateMutation.isPending ? 'Scanning…' : 'Generate alerts'}
        </Button>
      </div>

      {generateMutation.data && (
        <p className="text-sm text-muted-foreground">
          Created {generateMutation.data.alertsCreated} new alerts ·{' '}
          {generateMutation.data.openAlertCount} open
        </p>
      )}

      {alertsQuery.isLoading && <p className="text-muted-foreground">Loading alerts…</p>}
      <div className="space-y-3">
        {alertsQuery.data?.content.map((alert) => (
          <Card key={alert.id}>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-base">
                {alert.alertType}{' '}
                <span className="text-sm font-normal text-muted-foreground">
                  · {alert.severity}
                </span>
              </CardTitle>
              <Button
                size="sm"
                variant="outline"
                disabled={acknowledgeMutation.isPending}
                onClick={() => acknowledgeMutation.mutate(alert.id)}
              >
                Acknowledge
              </Button>
            </CardHeader>
            <CardContent className="space-y-1 text-sm">
              <p>{alert.message}</p>
              <p className="text-muted-foreground">
                {new Date(alert.detectedAt).toLocaleString()}
              </p>
            </CardContent>
          </Card>
        ))}
        {alertsQuery.data?.content.length === 0 && (
          <p className="text-muted-foreground">No open operator alerts.</p>
        )}
      </div>
    </div>
  );
}
