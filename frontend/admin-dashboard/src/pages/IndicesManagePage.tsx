import { FormEvent, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Loader2 } from 'lucide-react';
import { api } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

export function IndicesManagePage() {
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [symbol, setSymbol] = useState('');
  const [contractId, setContractId] = useState('');
  const [weightBps, setWeightBps] = useState('10000');

  const indicesQuery = useQuery({
    queryKey: ['admin-indices'],
    queryFn: () => api.listIndices(),
  });

  const createMutation = useMutation({
    mutationFn: () =>
      api.createIndex({
        name,
        symbol,
        constituents: [{ contractId, weightBps: Number(weightBps) }],
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-indices'] });
      setName('');
      setSymbol('');
      setContractId('');
    },
  });

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    createMutation.mutate();
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-6">
      <h1 className="text-2xl font-semibold">Index baskets</h1>

      <Card>
        <CardHeader>
          <CardTitle>Create index</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={onSubmit} className="grid gap-4 md:grid-cols-2">
            <div>
              <Label htmlFor="name">Name</Label>
              <Input id="name" value={name} onChange={(e) => setName(e.target.value)} required />
            </div>
            <div>
              <Label htmlFor="symbol">Symbol</Label>
              <Input id="symbol" value={symbol} onChange={(e) => setSymbol(e.target.value)} required />
            </div>
            <div>
              <Label htmlFor="contractId">Constituent contract ID</Label>
              <Input id="contractId" value={contractId} onChange={(e) => setContractId(e.target.value)} required />
            </div>
            <div>
              <Label htmlFor="weightBps">Weight (bps)</Label>
              <Input id="weightBps" type="number" value={weightBps} onChange={(e) => setWeightBps(e.target.value)} required />
            </div>
            <Button type="submit" disabled={createMutation.isPending} className="md:col-span-2">
              {createMutation.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Create index
            </Button>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Existing indices</CardTitle>
        </CardHeader>
        <CardContent>
          <ul className="divide-y text-sm">
            {indicesQuery.data?.map((index) => (
              <li key={index.id} className="py-2">
                {index.name} ({index.symbol}) — {index.status}
              </li>
            ))}
          </ul>
        </CardContent>
      </Card>
    </div>
  );
}
