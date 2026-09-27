import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export function IndicesPage() {
  const indicesQuery = useQuery({
    queryKey: ['indices'],
    queryFn: () => api.listIndices(),
  });

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Property indices</h1>
        <p className="text-muted-foreground">REIT-style baskets of underlying property tokens</p>
      </div>

      {indicesQuery.isLoading && <p className="text-muted-foreground">Loading indices…</p>}
      <div className="grid gap-4">
        {indicesQuery.data?.map((index) => (
          <Card key={index.id}>
            <CardHeader>
              <CardTitle>{index.name} ({index.symbol})</CardTitle>
            </CardHeader>
            <CardContent className="text-sm">
              <p className="text-muted-foreground">{index.description ?? 'No description'}</p>
              <p className="mt-2">Status: {index.status}</p>
              <p>Constituents: {index.constituents.length}</p>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
