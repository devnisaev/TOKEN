import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export function OperationsReportsPage() {
  const exportQuery = useQuery({
    queryKey: ['operations-export'],
    queryFn: () => api.getOperationsExport(),
  });

  const data = exportQuery.data;

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-6">
      <h1 className="text-2xl font-semibold">Operations reports</h1>
      <p className="text-muted-foreground">Alert summaries, health trends, and KPI snapshot</p>

      {exportQuery.isLoading && <p className="text-muted-foreground">Loading report…</p>}
      {data && (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">Open alerts</CardTitle>
              </CardHeader>
              <CardContent className="text-2xl font-bold">
                {data.alertSummary.openAlertCount}
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">Critical</CardTitle>
              </CardHeader>
              <CardContent className="text-2xl font-bold">
                {data.alertSummary.criticalOpenCount}
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">Declining health</CardTitle>
              </CardHeader>
              <CardContent className="text-2xl font-bold">{data.decliningHealthCount}</CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">Avg health</CardTitle>
              </CardHeader>
              <CardContent className="text-2xl font-bold">
                {data.kpis.averageHealthScore.toFixed(1)}%
              </CardContent>
            </Card>
          </div>

          <section className="space-y-3">
            <h2 className="text-lg font-medium">Lease coverage</h2>
            <div className="grid gap-4 sm:grid-cols-3">
              <Card>
                <CardContent className="py-4 text-sm">
                  <p className="text-muted-foreground">Active leases</p>
                  <p className="text-xl font-semibold">{data.leaseCoverage.activeLeaseCount}</p>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="py-4 text-sm">
                  <p className="text-muted-foreground">Expiring (30d)</p>
                  <p className="text-xl font-semibold">{data.leaseCoverage.expiringLeaseCount}</p>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="py-4 text-sm">
                  <p className="text-muted-foreground">Vacancy risk</p>
                  <p className="text-xl font-semibold">{data.leaseCoverage.vacancyRiskCount}</p>
                </CardContent>
              </Card>
            </div>
          </section>

          <section className="space-y-3">
            <h2 className="text-lg font-medium">Maintenance backlog</h2>
            <div className="grid gap-4 sm:grid-cols-3">
              <Card>
                <CardContent className="py-4 text-sm">
                  <p className="text-muted-foreground">Open tickets</p>
                  <p className="text-xl font-semibold">{data.maintenanceBacklog.totalOpenTicketCount}</p>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="py-4 text-sm">
                  <p className="text-muted-foreground">Backlog flats</p>
                  <p className="text-xl font-semibold">{data.maintenanceBacklog.backlogFlatCount}</p>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="py-4 text-sm">
                  <p className="text-muted-foreground">Rent collected</p>
                  <p className="text-xl font-semibold">
                    ${data.rentCollection.totalCollectedUsd.toLocaleString()}
                  </p>
                </CardContent>
              </Card>
            </div>
          </section>

          <section className="space-y-3">
            <h2 className="text-lg font-medium">Rent collection</h2>
            <div className="grid gap-4 sm:grid-cols-2">
              <Card>
                <CardContent className="py-4 text-sm">
                  <p className="text-muted-foreground">Collections recorded</p>
                  <p className="text-xl font-semibold">{data.rentCollection.collectionCount}</p>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="py-4 text-sm">
                  <p className="text-muted-foreground">Flats with rent</p>
                  <p className="text-xl font-semibold">{data.rentCollection.flatsWithRent}</p>
                </CardContent>
              </Card>
            </div>
          </section>

          <section className="space-y-3">
            <h2 className="text-lg font-medium">Alerts by type</h2>
            <div className="grid gap-2">
              {data.alertSummary.byType.map((item) => (
                <Card key={item.alertType}>
                  <CardContent className="flex justify-between py-3 text-sm">
                    <span>{item.alertType}</span>
                    <span className="font-medium">{item.count}</span>
                  </CardContent>
                </Card>
              ))}
              {data.alertSummary.byType.length === 0 && (
                <p className="text-muted-foreground">No alerts recorded.</p>
              )}
            </div>
          </section>

          <section className="space-y-3">
            <h2 className="text-lg font-medium">Declining health assets</h2>
            <div className="grid gap-2">
              {data.decliningHealth.map((item) => (
                <Card key={item.flatId}>
                  <CardContent className="flex flex-wrap gap-6 py-3 text-sm">
                    <div>
                      <p className="text-muted-foreground">Flat</p>
                      <p className="font-medium">{item.flatId.slice(0, 8)}…</p>
                    </div>
                    <div>
                      <p className="text-muted-foreground">Previous → latest</p>
                      <p className="font-medium">
                        {item.previousScore.toFixed(1)}% → {item.latestScore.toFixed(1)}%
                      </p>
                    </div>
                    <div>
                      <p className="text-muted-foreground">Delta</p>
                      <p className="font-medium">{item.delta.toFixed(1)}%</p>
                    </div>
                  </CardContent>
                </Card>
              ))}
              {data.decliningHealth.length === 0 && (
                <p className="text-muted-foreground">No declining health trends detected.</p>
              )}
            </div>
          </section>
        </>
      )}
    </div>
  );
}
