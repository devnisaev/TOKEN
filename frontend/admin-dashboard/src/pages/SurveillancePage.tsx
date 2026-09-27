import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export function SurveillancePage() {
  const alertsQuery = useQuery({
    queryKey: ['surveillance-alerts'],
    queryFn: () => api.listSurveillanceAlerts(),
  });

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-6">
      <h1 className="text-2xl font-semibold">Trade surveillance</h1>
      <p className="text-muted-foreground">Large trades, self-trades, and liquidation alerts</p>

      {alertsQuery.isLoading && <p className="text-muted-foreground">Loading alerts…</p>}
      <div className="space-y-3">
        {alertsQuery.data?.content.map((alert) => (
          <Card key={alert.id}>
            <CardHeader className="pb-2">
              <CardTitle className="text-base">{alert.alertType}</CardTitle>
            </CardHeader>
            <CardContent className="text-sm text-muted-foreground">
              Order {alert.orderId.slice(0, 8)}… · {new Date(alert.detectedAt).toLocaleString()}
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
