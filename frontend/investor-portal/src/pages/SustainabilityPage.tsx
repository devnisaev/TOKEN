import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export function SustainabilityPage() {
  const { flatId } = useParams<{ flatId: string }>();

  const esgQuery = useQuery({
    queryKey: ['esg-flat', flatId],
    queryFn: () => api.getEsgByFlat(flatId!),
    enabled: !!flatId,
  });

  const flatQuery = useQuery({
    queryKey: ['flat-bff', flatId],
    queryFn: () => api.getFlatDetail(flatId!),
    enabled: !!flatId,
  });

  const healthQuery = useQuery({
    queryKey: ['portfolio-health', flatId],
    queryFn: () => api.getPortfolioHealth([flatId!]),
    enabled: !!flatId,
  });

  if (!flatId) {
    return <p className="text-destructive p-4">Missing flat id.</p>;
  }

  const esg = esgQuery.data;

  return (
    <div className="mx-auto max-w-2xl space-y-6 p-4">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Sustainability</h1>
        <p className="text-muted-foreground">
          {flatQuery.data?.buildingName ?? 'Asset'} · ESG & utilization
        </p>
      </div>

      {esgQuery.isLoading && <p className="text-muted-foreground">Loading ESG profile…</p>}
      {esgQuery.isError && (
        <p className="text-muted-foreground">No ESG profile published for this asset yet.</p>
      )}

      {healthQuery.data?.[0]?.healthScore != null && (
        <Card>
          <CardHeader>
            <CardTitle>Asset health score</CardTitle>
          </CardHeader>
          <CardContent className="grid grid-cols-3 gap-4 text-sm">
            <div>
              <p className="text-muted-foreground">Composite score</p>
              <p className="text-lg font-semibold">
                {healthQuery.data[0].healthScore!.toFixed(1)}%
              </p>
            </div>
            <div>
              <p className="text-muted-foreground">ESG factor</p>
              <p className="text-lg font-semibold">{healthQuery.data[0].esgFactor ?? '—'}</p>
            </div>
            <div>
              <p className="text-muted-foreground">Occupancy factor</p>
              <p className="text-lg font-semibold">{healthQuery.data[0].occupancyFactor ?? '—'}</p>
            </div>
          </CardContent>
        </Card>
      )}

      {esg && (
        <Card>
          <CardHeader>
            <CardTitle>ESG profile</CardTitle>
          </CardHeader>
          <CardContent className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <p className="text-muted-foreground">Carbon score</p>
              <p className="text-lg font-semibold">{esg.carbonScore ?? '—'}</p>
            </div>
            <div>
              <p className="text-muted-foreground">Energy rating</p>
              <p className="text-lg font-semibold">{esg.energyRating ?? '—'}</p>
            </div>
            <div>
              <p className="text-muted-foreground">Environmental risk</p>
              <p className="text-lg font-semibold">{esg.environmentalRiskTier ?? '—'}</p>
            </div>
            <div>
              <p className="text-muted-foreground">Last assessed</p>
              <p className="text-lg font-semibold">
                {esg.lastAssessedAt ? new Date(esg.lastAssessedAt).toLocaleDateString() : '—'}
              </p>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
