import { Link, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ArrowLeft, Loader2, Plus } from 'lucide-react';
import { api } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export function BuildingDetailPage() {
  const { buildingId } = useParams<{ buildingId: string }>();

  const { data, isLoading, error } = useQuery({
    queryKey: ['building-bff', buildingId],
    queryFn: () => api.getBuildingBff(buildingId!),
    enabled: !!buildingId,
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-20 text-muted-foreground">
        <Loader2 className="mr-2 h-5 w-5 animate-spin" />
        Loading building…
      </div>
    );
  }

  if (error || !data) {
    return (
      <p className="text-destructive">{error instanceof Error ? error.message : 'Building not found'}</p>
    );
  }

  const { building, tokenizedFlatCount, availableFlatCount } = data;

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Link to="/buildings" className="text-muted-foreground hover:text-foreground">
          <ArrowLeft className="h-5 w-5" />
        </Link>
        <div className="flex-1">
          <h1 className="text-3xl font-bold tracking-tight">{building.name}</h1>
          <p className="text-muted-foreground">
            {building.address}, {building.city}, {building.country}
          </p>
        </div>
        <Link to={`/buildings/${buildingId}/edit`}>
          <Button variant="outline">Edit building</Button>
        </Link>
      </div>

      <div className="grid gap-4 sm:grid-cols-4">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Total flats</CardTitle>
          </CardHeader>
          <CardContent className="text-2xl font-bold">{building.flatCount ?? 0}</CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Tokenized</CardTitle>
          </CardHeader>
          <CardContent className="text-2xl font-bold">{tokenizedFlatCount}</CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Available</CardTitle>
          </CardHeader>
          <CardContent className="text-2xl font-bold">{availableFlatCount}</CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Status</CardTitle>
          </CardHeader>
          <CardContent className="text-2xl font-bold">{building.status}</CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Property metadata</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-2 text-sm sm:grid-cols-2">
          <p>
            <span className="text-muted-foreground">Category:</span>{' '}
            {building.propertyCategory ?? '—'}
          </p>
          <p>
            <span className="text-muted-foreground">Cadastral ref:</span>{' '}
            {building.cadastralReference ?? '—'}
          </p>
          <p>
            <span className="text-muted-foreground">Construction year:</span>{' '}
            {building.constructionYear ?? '—'}
          </p>
          <p>
            <span className="text-muted-foreground">Last renovation:</span>{' '}
            {building.lastRenovationYear ?? '—'}
          </p>
          <p>
            <span className="text-muted-foreground">Energy rating:</span>{' '}
            {building.energyEfficiencyRating ?? '—'}
          </p>
          <p>
            <span className="text-muted-foreground">Zoning code:</span>{' '}
            {building.zoningCode ?? '—'}
          </p>
          <p>
            <span className="text-muted-foreground">Total area:</span>{' '}
            {building.totalAreaSqm != null ? `${building.totalAreaSqm} m²` : '—'}
          </p>
        </CardContent>
      </Card>

      {building.spv && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">SPV</CardTitle>
          </CardHeader>
          <CardContent className="space-y-1 text-sm">
            <p>
              <span className="text-muted-foreground">Legal name:</span> {building.spv.legalName}
            </p>
            <p>
              <span className="text-muted-foreground">Registration:</span>{' '}
              {building.spv.registrationNumber}
            </p>
            <p>
              <span className="text-muted-foreground">KYC:</span>{' '}
              {building.spv.kycVerified ? 'Verified' : 'Pending'}
            </p>
          </CardContent>
        </Card>
      )}

      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold">Flats</h2>
        <Link to={`/buildings/${buildingId}/flats/new`}>
          <Button size="sm">
            <Plus className="mr-1 h-4 w-4" />
            Add flat
          </Button>
        </Link>
      </div>

      <div className="overflow-x-auto rounded-lg border">
        <table className="w-full text-sm">
          <thead className="border-b bg-muted/50">
            <tr>
              <th className="px-4 py-3 text-left font-medium">Unit</th>
              <th className="px-4 py-3 text-left font-medium">Floor</th>
              <th className="px-4 py-3 text-left font-medium">Area m²</th>
              <th className="px-4 py-3 text-left font-medium">Status</th>
              <th className="px-4 py-3 text-left font-medium">Token price</th>
              <th className="px-4 py-3 text-right font-medium">Actions</th>
            </tr>
          </thead>
          <tbody>
            {(building.flats ?? []).map((flat) => (
              <tr key={flat.id} className="border-b last:border-0">
                <td className="px-4 py-3 font-medium">{flat.flatNumber}</td>
                <td className="px-4 py-3">{flat.floor ?? '—'}</td>
                <td className="px-4 py-3">{flat.areaSqm ?? '—'}</td>
                <td className="px-4 py-3">
                  <span className="rounded-md bg-secondary px-2 py-0.5 text-xs">{flat.status}</span>
                </td>
                <td className="px-4 py-3">
                  {flat.tokenPriceUsd != null ? `$${flat.tokenPriceUsd}` : '—'}
                </td>
                <td className="px-4 py-3 text-right">
                  <div className="flex justify-end gap-3">
                    {flat.status === 'AVAILABLE' && (
                      <Link
                        to={`/buildings/${buildingId}/flats/${flat.id}/tokenize`}
                        className="text-primary underline-offset-4 hover:underline"
                      >
                        Tokenize
                      </Link>
                    )}
                    <Link
                      to={`/buildings/${buildingId}/flats/${flat.id}/edit`}
                      className="text-primary underline-offset-4 hover:underline"
                    >
                      Edit
                    </Link>
                  </div>
                </td>
              </tr>
            ))}
            {(building.flats ?? []).length === 0 && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-muted-foreground">
                  No flats registered yet.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
