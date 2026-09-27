import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { ArrowLeft } from 'lucide-react';
import { api } from '@/lib/api';
import type { AssetPropertyCategory, CreateStandaloneAssetRequest } from '@/types/api';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

const CATEGORIES: AssetPropertyCategory[] = [
  'SINGLE_FAMILY_HOUSE',
  'LAND_PARCEL',
  'GYM',
  'SWIMMING_POOL',
  'SERVICE_STATION',
  'HOSPITALITY',
  'CAR_WASH',
  'INDUSTRIAL_WAREHOUSE',
];

const emptyForm: CreateStandaloneAssetRequest = {
  name: '',
  address: '',
  city: '',
  country: 'KG',
  propertyCategory: 'GYM',
  unitLabel: 'UNIT-1',
  areaSqm: 500,
  operatingModel: 'OPERATOR_REVENUE_SHARE',
  liquidityTier: 'TIER_2',
};

export function StandaloneAssetFormPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState(emptyForm);

  const saveMutation = useMutation({
    mutationFn: () => api.createStandaloneAsset(form),
    onSuccess: (data) => navigate(`/buildings/${data.building.id}`),
  });

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    saveMutation.mutate();
  }

  function setField<K extends keyof CreateStandaloneAssetRequest>(
    key: K,
    value: CreateStandaloneAssetRequest[K],
  ) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  return (
    <div className="mx-auto max-w-lg space-y-6">
      <Link
        to="/buildings"
        className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="mr-1 h-4 w-4" />
        Back to buildings
      </Link>

      <Card>
        <CardHeader>
          <CardTitle>Register standalone asset</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={onSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="name">Asset name</Label>
              <Input
                id="name"
                required
                value={form.name}
                onChange={(e) => setField('name', e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="propertyCategory">Category</Label>
              <select
                id="propertyCategory"
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                value={form.propertyCategory}
                onChange={(e) => setField('propertyCategory', e.target.value as AssetPropertyCategory)}
              >
                {CATEGORIES.map((c) => (
                  <option key={c} value={c}>
                    {c.replace(/_/g, ' ')}
                  </option>
                ))}
              </select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="address">Address</Label>
              <Input
                id="address"
                required
                value={form.address}
                onChange={(e) => setField('address', e.target.value)}
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="city">City</Label>
                <Input
                  id="city"
                  required
                  value={form.city}
                  onChange={(e) => setField('city', e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="areaSqm">Area (m²)</Label>
                <Input
                  id="areaSqm"
                  type="number"
                  required
                  value={form.areaSqm}
                  onChange={(e) => setField('areaSqm', Number(e.target.value))}
                />
              </div>
            </div>
            {saveMutation.error && (
              <p className="text-sm text-destructive">
                {saveMutation.error instanceof Error ? saveMutation.error.message : 'Save failed'}
              </p>
            )}
            <Button type="submit" disabled={saveMutation.isPending} className="w-full">
              {saveMutation.isPending ? 'Creating…' : 'Create standalone asset'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
