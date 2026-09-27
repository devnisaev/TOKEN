import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

function formatPct(value: number) {
  return `${value.toFixed(1)}%`;
}

export function OperatorKpisPage() {
  const kpisQuery = useQuery({
    queryKey: ['operator-kpis'],
    queryFn: () => api.getOperatorKpis(),
  });
  const healthQuery = useQuery({
    queryKey: ['asset-health-scores'],
    queryFn: () => api.listAssetHealthScores(),
  });
  const expiryQuery = useQuery({
    queryKey: ['insurance-expiry', 30],
    queryFn: () => api.listInsuranceExpiryAlerts(30),
  });

  const kpis = kpisQuery.data;

  return (
    <div className="mx-auto max-w-5xl space-y-6 p-6">
      <h1 className="text-2xl font-semibold">Operator KPIs</h1>
      <p className="text-muted-foreground">
        Portfolio health, occupancy, and insurance expiry alerts
      </p>

      {kpisQuery.isLoading && <p className="text-muted-foreground">Loading KPIs…</p>}
      {kpis && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                Avg occupancy
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-semibold">{formatPct(kpis.averageOccupancyPct)}</p>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                Avg carbon score
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-semibold">{kpis.averageCarbonScore.toFixed(1)}</p>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                Avg health score
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-semibold">{formatPct(kpis.averageHealthScore)}</p>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                At-risk assets
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-semibold">
                {kpis.atRiskAssetCount} / {kpis.trackedAssetCount}
              </p>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                Open alerts
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-semibold">{kpis.openOperatorAlertCount}</p>
            </CardContent>
          </Card>
        </div>
      )}

      <section className="space-y-3">
        <h2 className="text-lg font-medium">Asset health scores</h2>
        {healthQuery.isLoading && <p className="text-muted-foreground">Loading health scores…</p>}
        <div className="grid gap-3">
          {healthQuery.data?.map((score) => (
            <Card key={score.id}>
              <CardContent className="flex flex-wrap items-center gap-6 py-4 text-sm">
                <div>
                  <p className="text-muted-foreground">Building</p>
                  <p className="font-medium">{score.buildingId.slice(0, 8)}…</p>
                </div>
                <div>
                  <p className="text-muted-foreground">Health score</p>
                  <p className="font-medium">{formatPct(score.healthScore)}</p>
                </div>
                <div>
                  <p className="text-muted-foreground">ESG / Occ / Ins</p>
                  <p className="font-medium">
                    {score.esgFactor} / {score.occupancyFactor} / {score.insuranceFactor}
                  </p>
                </div>
              </CardContent>
            </Card>
          ))}
          {healthQuery.data?.length === 0 && (
            <p className="text-muted-foreground">No health scores recorded yet.</p>
          )}
        </div>
      </section>

      <section className="space-y-3">
        <h2 className="text-lg font-medium">Insurance expiring (30 days)</h2>
        {expiryQuery.isLoading && <p className="text-muted-foreground">Loading expiry alerts…</p>}
        <div className="grid gap-3">
          {expiryQuery.data?.map((alert) => (
            <Card key={alert.id}>
              <CardContent className="flex flex-wrap items-center gap-6 py-4 text-sm">
                <div>
                  <p className="text-muted-foreground">Provider</p>
                  <p className="font-medium">{alert.provider}</p>
                </div>
                <div>
                  <p className="text-muted-foreground">Policy</p>
                  <p className="font-medium">{alert.policyNumber}</p>
                </div>
                <div>
                  <p className="text-muted-foreground">Days until expiry</p>
                  <p className="font-medium">{alert.daysUntilExpiry}</p>
                </div>
                <div>
                  <p className="text-muted-foreground">Coverage</p>
                  <p className="font-medium">
                    ${alert.coverageUsd.toLocaleString(undefined, { maximumFractionDigits: 0 })}
                  </p>
                </div>
              </CardContent>
            </Card>
          ))}
          {expiryQuery.data?.length === 0 && (
            <p className="text-muted-foreground">No policies expiring within 30 days.</p>
          )}
        </div>
      </section>
    </div>
  );
}
