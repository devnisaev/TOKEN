import { FormEvent, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery } from '@tanstack/react-query';
import { ArrowLeft, Loader2 } from 'lucide-react';
import { api } from '@/lib/api';
import type { CreateBuildingRequest } from '@/types/api';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

const emptyForm: CreateBuildingRequest = {
  name: '',
  address: '',
  city: '',
  country: 'KG',
  postalCode: '',
  totalFloors: undefined,
  totalFlats: undefined,
  constructionYear: undefined,
  totalAreaSqm: undefined,
  propertyCategory: undefined,
  cadastralReference: '',
  energyEfficiencyRating: '',
  zoningCode: '',
  lastRenovationYear: undefined,
};

const PROPERTY_CATEGORIES = [
  'RESIDENTIAL_FLAT',
  'COMMERCIAL_BUILDING',
  'SINGLE_FAMILY_HOUSE',
  'LAND_PARCEL',
  'INDUSTRIAL_WAREHOUSE',
] as const;

export function BuildingFormPage() {
  const { buildingId } = useParams<{ buildingId: string }>();
  const isEdit = !!buildingId;
  const navigate = useNavigate();
  const [form, setForm] = useState<CreateBuildingRequest>(emptyForm);

  const buildingQuery = useQuery({
    queryKey: ['building', buildingId],
    queryFn: () => api.getBuilding(buildingId!),
    enabled: isEdit,
  });

  useEffect(() => {
    if (buildingQuery.data) {
      const b = buildingQuery.data;
      setForm({
        name: b.name,
        address: b.address,
        city: b.city,
        country: b.country,
        postalCode: b.postalCode ?? '',
        totalFloors: b.totalFloors,
        totalFlats: b.totalFlats,
        constructionYear: b.constructionYear,
        totalAreaSqm: b.totalAreaSqm,
        propertyCategory: b.propertyCategory,
        cadastralReference: b.cadastralReference ?? '',
        energyEfficiencyRating: b.energyEfficiencyRating ?? '',
        zoningCode: b.zoningCode ?? '',
        lastRenovationYear: b.lastRenovationYear,
      });
    }
  }, [buildingQuery.data]);

  const saveMutation = useMutation({
    mutationFn: () =>
      isEdit ? api.updateBuilding(buildingId!, form) : api.createBuilding(form),
    onSuccess: () => navigate('/buildings'),
  });

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    saveMutation.mutate();
  }

  function setField<K extends keyof CreateBuildingRequest>(key: K, value: CreateBuildingRequest[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  if (isEdit && buildingQuery.isLoading) {
    return (
      <div className="flex items-center justify-center py-20 text-muted-foreground">
        <Loader2 className="mr-2 h-5 w-5 animate-spin" />
        Loading building…
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-lg space-y-6">
      <Link to="/buildings" className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="mr-1 h-4 w-4" /> Buildings
      </Link>

      <Card>
        <CardHeader>
          <CardTitle>{isEdit ? 'Edit building' : 'Register building'}</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={onSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="name">Name</Label>
              <Input id="name" value={form.name} onChange={(e) => setField('name', e.target.value)} required />
            </div>
            <div className="space-y-2">
              <Label htmlFor="address">Address</Label>
              <Input id="address" value={form.address} onChange={(e) => setField('address', e.target.value)} required />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="city">City</Label>
                <Input id="city" value={form.city} onChange={(e) => setField('city', e.target.value)} required />
              </div>
              <div className="space-y-2">
                <Label htmlFor="country">Country</Label>
                <Input id="country" value={form.country} onChange={(e) => setField('country', e.target.value)} required />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="postalCode">Postal code</Label>
              <Input id="postalCode" value={form.postalCode ?? ''} onChange={(e) => setField('postalCode', e.target.value)} />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="totalFloors">Total floors</Label>
                <Input
                  id="totalFloors"
                  type="number"
                  min={1}
                  value={form.totalFloors ?? ''}
                  onChange={(e) => setField('totalFloors', e.target.value ? Number(e.target.value) : undefined)}
                />
              </div>
              {!isEdit && (
                <div className="space-y-2">
                  <Label htmlFor="totalFlats">Total flats</Label>
                  <Input
                    id="totalFlats"
                    type="number"
                    min={1}
                    value={form.totalFlats ?? ''}
                    onChange={(e) => setField('totalFlats', e.target.value ? Number(e.target.value) : undefined)}
                  />
                </div>
              )}
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="constructionYear">Construction year</Label>
                <Input
                  id="constructionYear"
                  type="number"
                  min={1800}
                  max={2100}
                  value={form.constructionYear ?? ''}
                  onChange={(e) =>
                    setField('constructionYear', e.target.value ? Number(e.target.value) : undefined)
                  }
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="totalAreaSqm">Total area m²</Label>
                <Input
                  id="totalAreaSqm"
                  type="number"
                  min={0}
                  step="0.1"
                  value={form.totalAreaSqm ?? ''}
                  onChange={(e) =>
                    setField('totalAreaSqm', e.target.value ? Number(e.target.value) : undefined)
                  }
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="propertyCategory">Property category</Label>
              <select
                id="propertyCategory"
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                value={form.propertyCategory ?? ''}
                onChange={(e) =>
                  setField('propertyCategory', e.target.value || undefined)
                }
              >
                <option value="">Select category</option>
                {PROPERTY_CATEGORIES.map((cat) => (
                  <option key={cat} value={cat}>
                    {cat.replace(/_/g, ' ')}
                  </option>
                ))}
              </select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="cadastralReference">Cadastral reference</Label>
              <Input
                id="cadastralReference"
                value={form.cadastralReference ?? ''}
                onChange={(e) => setField('cadastralReference', e.target.value)}
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="energyEfficiencyRating">Energy rating</Label>
                <Input
                  id="energyEfficiencyRating"
                  placeholder="e.g. A+"
                  value={form.energyEfficiencyRating ?? ''}
                  onChange={(e) => setField('energyEfficiencyRating', e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="zoningCode">Zoning code</Label>
                <Input
                  id="zoningCode"
                  value={form.zoningCode ?? ''}
                  onChange={(e) => setField('zoningCode', e.target.value)}
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="lastRenovationYear">Last renovation year</Label>
              <Input
                id="lastRenovationYear"
                type="number"
                min={1800}
                max={2100}
                value={form.lastRenovationYear ?? ''}
                onChange={(e) =>
                  setField('lastRenovationYear', e.target.value ? Number(e.target.value) : undefined)
                }
              />
            </div>
            {saveMutation.error && (
              <p className="text-sm text-destructive">
                {saveMutation.error instanceof Error ? saveMutation.error.message : 'Save failed'}
              </p>
            )}
            <Button type="submit" className="w-full" disabled={saveMutation.isPending}>
              {saveMutation.isPending ? 'Saving…' : isEdit ? 'Update building' : 'Create building'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
