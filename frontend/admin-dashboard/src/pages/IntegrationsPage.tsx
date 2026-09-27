import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Loader2 } from 'lucide-react';
import { api } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export function IntegrationsPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['integration-credentials'],
    queryFn: () => api.listIntegrationCredentials(),
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Integrations</h1>
          <p className="text-muted-foreground">Hub credentials for valuation feeds and custody</p>
        </div>
        <Link to="/integrations/deliveries">
          <Button variant="outline">Webhook deliveries</Button>
        </Link>
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
        {data?.content.map((cred) => (
          <li key={cred.id}>
            <Card>
              <CardHeader>
                <CardTitle className="text-base">
                  {cred.integrationType} · {cred.provider}
                </CardTitle>
              </CardHeader>
              <CardContent className="text-sm text-muted-foreground">
                Version {cred.version}
                {cred.rotatedAt && ` · rotated ${new Date(cred.rotatedAt).toLocaleString()}`}
              </CardContent>
            </Card>
          </li>
        ))}
      </ul>

      {data && data.content.length === 0 && (
        <Card>
          <CardContent className="py-8 text-center text-muted-foreground">No credentials configured.</CardContent>
        </Card>
      )}
    </div>
  );
}
