import { FormEvent, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Loader2 } from 'lucide-react';
import { api } from '@/lib/api';
import { formatUsd } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import type { LiquidityPool } from '@/types/api';

export function PoolsPage() {
  const queryClient = useQueryClient();
  const [contractId, setContractId] = useState('');
  const [flatId, setFlatId] = useState('');
  const [buildingId, setBuildingId] = useState('');

  const poolsQuery = useQuery({
    queryKey: ['liquidity-pools'],
    queryFn: () => api.listLiquidityPools(),
  });

  const createMutation = useMutation({
    mutationFn: () =>
      api.createLiquidityPool({
        contractId,
        flatId,
        buildingId,
        feeBps: 30,
        navBreakPct: 10,
        liquidityTier: 'TIER_1',
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['liquidity-pools'] });
      setContractId('');
      setFlatId('');
      setBuildingId('');
    },
  });

  function onCreate(e: FormEvent) {
    e.preventDefault();
    createMutation.mutate();
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-6">
      <h1 className="text-2xl font-semibold">Liquidity pools</h1>

      <Card>
        <CardHeader>
          <CardTitle>Create pool (TIER_1)</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={onCreate} className="grid gap-4 md:grid-cols-3">
            <div>
              <Label htmlFor="contractId">Contract ID</Label>
              <Input id="contractId" value={contractId} onChange={(e) => setContractId(e.target.value)} required />
            </div>
            <div>
              <Label htmlFor="flatId">Flat ID</Label>
              <Input id="flatId" value={flatId} onChange={(e) => setFlatId(e.target.value)} required />
            </div>
            <div>
              <Label htmlFor="buildingId">Building ID</Label>
              <Input id="buildingId" value={buildingId} onChange={(e) => setBuildingId(e.target.value)} required />
            </div>
            <Button type="submit" disabled={createMutation.isPending} className="md:col-span-3">
              {createMutation.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Create pool
            </Button>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Active pools</CardTitle>
        </CardHeader>
        <CardContent>
          {poolsQuery.isLoading && <p className="text-muted-foreground">Loading…</p>}
          {poolsQuery.data?.length === 0 && <p className="text-muted-foreground">No pools yet.</p>}
          <ul className="divide-y">
            {poolsQuery.data?.map((pool: LiquidityPool) => (
              <li key={pool.id} className="flex flex-wrap items-center justify-between gap-2 py-3 text-sm">
                <div>
                  <p className="font-medium">{pool.contractId}</p>
                  <p className="text-muted-foreground">
                    {pool.tokenReserve.toLocaleString()} tokens · {formatUsd(pool.usdcReserve)} USDC
                  </p>
                </div>
                <div className="text-right">
                  <p>{formatUsd(pool.spotPriceUsd)} / token</p>
                  <p className="text-muted-foreground">{pool.status}</p>
                </div>
              </li>
            ))}
          </ul>
        </CardContent>
      </Card>
    </div>
  );
}
