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
};

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
        cadastralReference: b.cadastralReference,
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
