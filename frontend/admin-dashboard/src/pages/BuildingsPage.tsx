import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Loader2, Plus } from 'lucide-react';
import { api } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export function BuildingsPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['buildings'],
    queryFn: () => api.listBuildings(),
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Buildings</h1>
          <p className="text-muted-foreground">Property registry catalog</p>
        </div>
        <Link to="/buildings/new">
          <Button>
            <Plus className="mr-1 h-4 w-4" />
            Add building
          </Button>
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

      <div className="grid gap-4 sm:grid-cols-2">
        {data?.content.map((b) => (
          <Card key={b.id}>
            <CardHeader>
              <CardTitle className="text-lg">{b.name}</CardTitle>
              <p className="text-sm text-muted-foreground">
                {b.city}, {b.country}
              </p>
            </CardHeader>
            <CardContent className="flex items-center justify-between text-sm">
              <span>{b.flatCount} flats</span>
              <div className="flex items-center gap-2">
                <span className="rounded-md bg-secondary px-2 py-0.5 text-xs font-medium">{b.status}</span>
                <Link to={`/buildings/${b.id}`} className="text-xs text-primary underline-offset-4 hover:underline">
                  View
                </Link>
                <Link to={`/buildings/${b.id}/edit`} className="text-xs text-primary underline-offset-4 hover:underline">
                  Edit
                </Link>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
