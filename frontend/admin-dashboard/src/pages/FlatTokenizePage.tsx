import { FormEvent, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery } from '@tanstack/react-query';
import { ArrowLeft, Loader2 } from 'lucide-react';
import { api } from '@/lib/api';
import type { IssueTokenRequest } from '@/types/api';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

export function FlatTokenizePage() {
  const { buildingId, flatId } = useParams<{ buildingId: string; flatId: string }>();
  const navigate = useNavigate();
  const [form, setForm] = useState<IssueTokenRequest | null>(null);

  const buildingQuery = useQuery({
    queryKey: ['building-bff', buildingId],
    queryFn: () => api.getBuildingBff(buildingId!),
    enabled: !!buildingId,
  });

  const flatQuery = useQuery({
    queryKey: ['flat', flatId],
    queryFn: () => api.getFlat(flatId!),
    enabled: !!flatId,
  });

  useEffect(() => {
    if (!buildingId || !flatId || !flatQuery.data || !buildingQuery.data) {
      return;
    }
    const flat = flatQuery.data;
    const building = buildingQuery.data.building;
    const spvWallet = building.spv?.walletAddress ?? '';
    setForm({
      flatId,
      buildingId,
      tokenName: `${building.name} Unit ${flat.flatNumber}`,
      tokenSymbol: `TR${flat.flatNumber.replace(/\W/g, '').slice(0, 8).toUpperCase() || 'FLAT'}`,
      totalSupply: 1000,
      tokenPriceUsd: 100,
      spvWalletAddress: spvWallet,
    });
  }, [buildingId, flatId, flatQuery.data, buildingQuery.data]);

  const tokenizeMutation = useMutation({
    mutationFn: (body: IssueTokenRequest) => api.issueTokens(body),
    onSuccess: () => navigate(`/buildings/${buildingId}`),
  });

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!form) return;
    tokenizeMutation.mutate(form);
  }

  function setField<K extends keyof IssueTokenRequest>(key: K, value: IssueTokenRequest[K]) {
    setForm((prev) => (prev ? { ...prev, [key]: value } : prev));
  }

  if (buildingQuery.isLoading || flatQuery.isLoading || !form) {
    return (
      <div className="flex items-center justify-center py-20 text-muted-foreground">
        <Loader2 className="mr-2 h-5 w-5 animate-spin" />
        Loading flat…
      </div>
    );
  }

  if (buildingQuery.error || flatQuery.error || !flatQuery.data) {
    return (
      <p className="text-destructive">
        {buildingQuery.error instanceof Error
          ? buildingQuery.error.message
          : flatQuery.error instanceof Error
            ? flatQuery.error.message
            : 'Flat not found'}
      </p>
    );
  }

  const flat = flatQuery.data;
  const building = buildingQuery.data!.building;

  return (
    <div className="mx-auto max-w-lg space-y-6">
      <Link
        to={`/buildings/${buildingId}`}
        className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="mr-1 h-4 w-4" />
        Back to building
      </Link>

      <div>
        <h1 className="text-3xl font-bold tracking-tight">Tokenize flat</h1>
        <p className="text-muted-foreground">
          {building.name} · Unit {flat.flatNumber} · {flat.status}
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Issue ERC-1400 tokens</CardTitle>
          <CardDescription>
            Deploys a property token contract via Token Issuance. SPV wallet receives rental income.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={onSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="tokenName">Token name</Label>
              <Input
                id="tokenName"
                value={form.tokenName}
                onChange={(e) => setField('tokenName', e.target.value)}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="tokenSymbol">Token symbol</Label>
              <Input
                id="tokenSymbol"
                value={form.tokenSymbol}
                maxLength={20}
                onChange={(e) => setField('tokenSymbol', e.target.value)}
                required
              />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="totalSupply">Total supply</Label>
                <Input
                  id="totalSupply"
                  type="number"
                  min={1}
                  value={form.totalSupply}
                  onChange={(e) => setField('totalSupply', Number(e.target.value))}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="tokenPriceUsd">Price per token (USD)</Label>
                <Input
                  id="tokenPriceUsd"
                  type="number"
                  min={0.01}
                  step="0.01"
                  value={form.tokenPriceUsd}
                  onChange={(e) => setField('tokenPriceUsd', Number(e.target.value))}
                  required
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="spvWalletAddress">SPV wallet address</Label>
              <Input
                id="spvWalletAddress"
                value={form.spvWalletAddress}
                onChange={(e) => setField('spvWalletAddress', e.target.value)}
                required
              />
              {!building.spv?.walletAddress && (
                <p className="text-xs text-amber-700">
                  Building SPV has no wallet on file — enter the SPV wallet manually.
                </p>
              )}
            </div>

            {tokenizeMutation.error && (
              <p className="text-sm text-destructive">
                {tokenizeMutation.error instanceof Error
                  ? tokenizeMutation.error.message
                  : 'Token issuance failed'}
              </p>
            )}

            <Button type="submit" className="w-full" disabled={tokenizeMutation.isPending}>
              {tokenizeMutation.isPending ? 'Issuing tokens…' : 'Issue tokens'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
