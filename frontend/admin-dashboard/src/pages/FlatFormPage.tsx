import { FormEvent, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery } from '@tanstack/react-query';
import { ArrowLeft, Loader2 } from 'lucide-react';
import { api } from '@/lib/api';
import type { CreateFlatRequest } from '@/types/api';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

const emptyForm: CreateFlatRequest = {
  flatNumber: '',
  floor: 1,
  areaSqm: 50,
  numRooms: 2,
  numBathrooms: 1,
};

export function FlatFormPage() {
  const { buildingId, flatId } = useParams<{ buildingId: string; flatId: string }>();
  const isEdit = !!flatId;
  const navigate = useNavigate();
  const [form, setForm] = useState<CreateFlatRequest>(emptyForm);

  const flatQuery = useQuery({
    queryKey: ['flat', flatId],
    queryFn: () => api.getFlat(flatId!),
    enabled: isEdit,
  });

  useEffect(() => {
    if (flatQuery.data) {
      const f = flatQuery.data;
      setForm({
        flatNumber: f.flatNumber ?? '',
        floor: f.floor ?? 1,
        areaSqm: f.areaSqm ?? 50,
        numRooms: f.numRooms ?? 2,
        numBathrooms: f.numBathrooms ?? 1,
        cadastralReference: f.cadastralReference,
        netUsableAreaSqm: f.netUsableAreaSqm,
      });
    }
  }, [flatQuery.data]);

  const saveMutation = useMutation({
    mutationFn: () =>
      isEdit
        ? api.updateFlat(flatId!, form)
        : api.createFlat(buildingId!, form),
    onSuccess: () => navigate(`/buildings/${buildingId}`),
  });

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    saveMutation.mutate();
  }

  function setField<K extends keyof CreateFlatRequest>(key: K, value: CreateFlatRequest[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  if (isEdit && flatQuery.isLoading) {
    return (
      <div className="flex items-center justify-center py-20 text-muted-foreground">
        <Loader2 className="mr-2 h-5 w-5 animate-spin" />
        Loading flat…
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-lg space-y-6">
      <Link
        to={`/buildings/${buildingId}`}
        className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="mr-1 h-4 w-4" />
        Back to building
      </Link>

      <Card>
        <CardHeader>
          <CardTitle>{isEdit ? 'Edit flat' : 'Add flat'}</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={onSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="flatNumber">Unit number</Label>
              <Input
                id="flatNumber"
                required
                value={form.flatNumber}
                onChange={(e) => setField('flatNumber', e.target.value)}
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="floor">Floor</Label>
                <Input
                  id="floor"
                  type="number"
                  required
                  value={form.floor}
                  onChange={(e) => setField('floor', Number(e.target.value))}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="areaSqm">Area (m²)</Label>
                <Input
                  id="areaSqm"
                  type="number"
                  step="0.1"
                  required
                  value={form.areaSqm}
                  onChange={(e) => setField('areaSqm', Number(e.target.value))}
                />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="numRooms">Rooms</Label>
                <Input
                  id="numRooms"
                  type="number"
                  required
                  value={form.numRooms}
                  onChange={(e) => setField('numRooms', Number(e.target.value))}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="numBathrooms">Bathrooms</Label>
                <Input
                  id="numBathrooms"
                  type="number"
                  required
                  value={form.numBathrooms}
                  onChange={(e) => setField('numBathrooms', Number(e.target.value))}
                />
              </div>
            </div>
            {saveMutation.error && (
              <p className="text-sm text-destructive">
                {saveMutation.error instanceof Error ? saveMutation.error.message : 'Save failed'}
              </p>
            )}
            <Button type="submit" disabled={saveMutation.isPending} className="w-full">
              {saveMutation.isPending ? 'Saving…' : isEdit ? 'Update flat' : 'Create flat'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
