import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { ArrowRight, Loader2 } from 'lucide-react';
import { api } from '@/lib/api';
import { formatUsd } from '@/lib/utils';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';

export function ListingsPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['listings', 'ACTIVE'],
    queryFn: () => api.listListings('ACTIVE'),
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-20 text-muted-foreground">
        <Loader2 className="mr-2 h-5 w-5 animate-spin" />
        Loading listings…
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-lg border border-destructive/30 bg-destructive/5 p-4 text-destructive">
        {error instanceof Error ? error.message : 'Failed to load listings'}
      </div>
    );
  }

  const listings = data?.content ?? [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Investment listings</h1>
        <p className="mt-1 text-muted-foreground">
          Fractional real estate tokens — buy with USDC on the primary market
        </p>
      </div>

      {listings.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center text-muted-foreground">
            No active listings yet. Tokenize a flat in the admin flow to create one.
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {listings.map((listing) => (
            <Card key={listing.id} className="flex flex-col transition-shadow hover:shadow-md">
              <CardHeader>
                <CardTitle className="text-lg">{listing.title || 'Property listing'}</CardTitle>
                <CardDescription>
                  {listing.listingType} · {listing.tokensAvailable.toLocaleString()} tokens available
                </CardDescription>
              </CardHeader>
              <CardContent className="mt-auto space-y-4">
                <div>
                  <p className="text-2xl font-semibold text-primary">{formatUsd(listing.priceUsd)}</p>
                  <p className="text-xs text-muted-foreground">per token · min {listing.minInvestmentTokens} tokens</p>
                </div>
                <Link to={`/listings/${listing.id}`}>
                  <Button className="w-full">
                    View details
                    <ArrowRight className="h-4 w-4" />
                  </Button>
                </Link>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
