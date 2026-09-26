import { FormEvent, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, CheckCircle2, Loader2 } from 'lucide-react';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { formatUsd } from '@/lib/utils';
import { ConnectWalletButton } from '@/components/ConnectWalletButton';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useAccount } from 'wagmi';

export function ListingDetailPage() {
  const { listingId } = useParams<{ listingId: string }>();
  const { user } = useAuth();
  const { address } = useAccount();
  const queryClient = useQueryClient();

  const [tokenAmount, setTokenAmount] = useState('');
  const [orderSuccess, setOrderSuccess] = useState<string | null>(null);

  const { data, isLoading, error } = useQuery({
    queryKey: ['bff-listing', listingId],
    queryFn: () => api.getListingDetail(listingId!),
    enabled: !!listingId,
  });

  const buyMutation = useMutation({
    mutationFn: () => {
      const wallet = address ?? user?.walletAddress;
      if (!user || !wallet) {
        throw new Error('Connect a wallet or set wallet on your profile before buying');
      }
      const amount = parseInt(tokenAmount, 10);
      if (!amount || amount < 1) {
        throw new Error('Enter a valid token amount');
      }
      return api.placeBuyOrder({
        listingId: listingId!,
        buyerId: user.id,
        buyerWallet: wallet,
        tokenAmount: amount,
      });
    },
    onSuccess: (order) => {
      setOrderSuccess(order.id);
      queryClient.invalidateQueries({ queryKey: ['bff-listing', listingId] });
      queryClient.invalidateQueries({ queryKey: ['portfolio'] });
    },
  });

  function onBuy(e: FormEvent) {
    e.preventDefault();
    setOrderSuccess(null);
    buyMutation.mutate();
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-20 text-muted-foreground">
        <Loader2 className="mr-2 h-5 w-5 animate-spin" />
        Loading…
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="space-y-4">
        <Link to="/" className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground">
          <ArrowLeft className="mr-1 h-4 w-4" /> Back to listings
        </Link>
        <div className="rounded-lg border border-destructive/30 bg-destructive/5 p-4 text-destructive">
          {error instanceof Error ? error.message : 'Listing not found'}
        </div>
      </div>
    );
  }

  const { listing, buildingName, flatNumber, flatStatus, tokenContract } = data;
  const total = listing.priceUsd * (parseInt(tokenAmount, 10) || 0);

  return (
    <div className="space-y-6">
      <Link to="/" className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="mr-1 h-4 w-4" /> Back to listings
      </Link>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="space-y-4 lg:col-span-2">
          <div>
            <p className="text-sm font-medium text-primary">{buildingName} · Flat {flatNumber}</p>
            <h1 className="text-3xl font-bold tracking-tight">{listing.title}</h1>
            <p className="mt-1 text-muted-foreground">
              {listing.listingType} · {flatStatus} · {listing.tokensAvailable.toLocaleString()} tokens left
            </p>
          </div>

          {tokenContract && (
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Token contract</CardTitle>
                <CardDescription>
                  {tokenContract.tokenSymbol ?? 'Property token'} · {tokenContract.status}
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-1 font-mono text-xs text-muted-foreground">
                {tokenContract.contractAddress && <p>{tokenContract.contractAddress}</p>}
                <p>Supply: {tokenContract.totalSupply?.toLocaleString() ?? '—'} tokens</p>
                <p>Price: {formatUsd(tokenContract.tokenPriceUsd ?? listing.priceUsd)} / token</p>
              </CardContent>
            </Card>
          )}
        </div>

        <Card>
          <CardHeader>
            <CardTitle>Buy tokens</CardTitle>
            <CardDescription>{formatUsd(listing.priceUsd)} per token</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <ConnectWalletButton />

            {orderSuccess ? (
              <div className="flex items-start gap-2 rounded-md bg-accent p-3 text-sm text-accent-foreground">
                <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0" />
                <div>
                  <p className="font-medium">Order placed</p>
                  <p className="text-xs opacity-80">Order ID: {orderSuccess}</p>
                  <p className="mt-1 text-xs opacity-80">
                    Payment and on-chain transfer continue via Kafka when services are running.
                  </p>
                </div>
              </div>
            ) : (
              <form onSubmit={onBuy} className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="amount">Token amount</Label>
                  <Input
                    id="amount"
                    type="number"
                    min={listing.minInvestmentTokens}
                    max={listing.tokensAvailable}
                    placeholder={`Min ${listing.minInvestmentTokens}`}
                    value={tokenAmount}
                    onChange={(e) => setTokenAmount(e.target.value)}
                    required
                  />
                  {tokenAmount && (
                    <p className="text-sm text-muted-foreground">Total: {formatUsd(total)}</p>
                  )}
                </div>
                {buyMutation.error && (
                  <p className="text-sm text-destructive">
                    {buyMutation.error instanceof Error ? buyMutation.error.message : 'Order failed'}
                  </p>
                )}
                <Button type="submit" className="w-full" disabled={buyMutation.isPending}>
                  {buyMutation.isPending ? 'Placing order…' : 'Place buy order'}
                </Button>
              </form>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
