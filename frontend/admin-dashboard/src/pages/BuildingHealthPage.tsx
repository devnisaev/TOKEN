import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export function BuildingHealthPage() {
  const healthQuery = useQuery({
    queryKey: ['building-health'],
    queryFn: () => api.listBuildingHealth(),
  });

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-6">
      <h1 className="text-2xl font-semibold">Building health</h1>
      <p className="text-muted-foreground">Aggregate asset health scores by building</p>

      {healthQuery.isLoading && <p className="text-muted-foreground">Loading rollups…</p>}
      <div className="space-y-3">
        {healthQuery.data?.map((item) => (
          <Card key={item.buildingId}>
            <CardHeader className="pb-2">
              <CardTitle className="text-base">
                <Link
                  to={`/buildings/${item.buildingId}`}
                  className="hover:underline"
                >
                  Building {item.buildingId.slice(0, 8)}…
                </Link>
              </CardTitle>
            </CardHeader>
            <CardContent className="flex flex-wrap gap-6 text-sm">
              <div>
                <p className="text-muted-foreground">Avg health</p>
                <p className="font-medium">{item.averageHealthScore.toFixed(1)}%</p>
              </div>
              <div>
                <p className="text-muted-foreground">Assets tracked</p>
                <p className="font-medium">{item.assetCount}</p>
              </div>
              <div>
                <p className="text-muted-foreground">At risk</p>
                <p className="font-medium">{item.atRiskAssetCount}</p>
              </div>
            </CardContent>
          </Card>
        ))}
        {healthQuery.data?.length === 0 && (
          <p className="text-muted-foreground">No building health data recorded yet.</p>
        )}
      </div>
    </div>
  );
}
