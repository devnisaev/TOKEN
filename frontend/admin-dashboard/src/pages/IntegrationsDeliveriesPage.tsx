import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ArrowLeft, Loader2 } from 'lucide-react';
import { api } from '@/lib/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { StatusBadge } from '@tokenrealty/shared-ui';

export function IntegrationsDeliveriesPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['integration-deliveries'],
    queryFn: () => api.listIntegrationDeliveries(),
  });

  return (
    <div className="space-y-6">
      <Link to="/integrations" className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="mr-1 h-4 w-4" /> Integrations
      </Link>

      <div>
        <h1 className="text-3xl font-bold tracking-tight">Webhook deliveries</h1>
        <p className="text-muted-foreground">Integration Hub relay status</p>
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
        {data?.content.map((delivery) => (
          <li key={delivery.id}>
            <Card>
              <CardHeader className="flex flex-row items-start justify-between space-y-0">
                <CardTitle className="text-base">
                  {delivery.integrationType} · {delivery.provider}
                </CardTitle>
                <StatusBadge status={delivery.status} />
              </CardHeader>
              <CardContent className="text-sm text-muted-foreground">
                Attempts {delivery.attempts}
                {delivery.lastError && ` · ${delivery.lastError}`}
                {delivery.createdAt && ` · ${new Date(delivery.createdAt).toLocaleString()}`}
              </CardContent>
            </Card>
          </li>
        ))}
      </ul>
    </div>
  );
}
