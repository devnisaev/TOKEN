import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export function EsgPage() {
  const esgQuery = useQuery({
    queryKey: ['esg-profiles'],
    queryFn: () => api.listEsgProfiles(),
  });

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-6">
      <h1 className="text-2xl font-semibold">ESG & asset operations</h1>
      <p className="text-muted-foreground">Carbon scores, energy ratings, and environmental risk tiers</p>

      {esgQuery.isLoading && <p className="text-muted-foreground">Loading ESG profiles…</p>}
      <div className="grid gap-4">
        {esgQuery.data?.map((profile) => (
          <Card key={profile.id}>
            <CardHeader className="pb-2">
              <CardTitle className="text-base">
                Building {profile.buildingId.slice(0, 8)}…
                {profile.flatId ? ` · Flat ${profile.flatId.slice(0, 8)}…` : ''}
              </CardTitle>
            </CardHeader>
            <CardContent className="grid grid-cols-3 gap-4 text-sm">
              <div>
                <p className="text-muted-foreground">Carbon score</p>
                <p className="font-medium">{profile.carbonScore ?? '—'}</p>
              </div>
              <div>
                <p className="text-muted-foreground">Energy rating</p>
                <p className="font-medium">{profile.energyRating ?? '—'}</p>
              </div>
              <div>
                <p className="text-muted-foreground">Risk tier</p>
                <p className="font-medium">{profile.environmentalRiskTier ?? '—'}</p>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
