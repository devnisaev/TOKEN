import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { ArrowRight, Loader2 } from 'lucide-react';
import { api } from '@/lib/api';
import { formatUsd } from '@/lib/utils';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';

const TYPE_FILTERS = ['ALL', 'PRIMARY', 'SECONDARY'] as const;
type TypeFilter = (typeof TYPE_FILTERS)[number];

export function ListingsPage() {
  const [typeFilter, setTypeFilter] = useState<TypeFilter>('ALL');

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

  const listings = (data?.content ?? []).filter(
    (listing) => typeFilter === 'ALL' || listing.listingType === typeFilter,
  );

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Investment listings</h1>
        <p className="mt-1 text-muted-foreground">
          Fractional real estate tokens — primary and secondary market
        </p>
      </div>

      <div className="flex flex-wrap gap-2">
        {TYPE_FILTERS.map((type) => (
          <Button
            key={type}
            size="sm"
            variant={typeFilter === type ? 'default' : 'outline'}
            onClick={() => setTypeFilter(type)}
          >
            {type === 'ALL' ? 'All' : type.charAt(0) + type.slice(1).toLowerCase()}
          </Button>
        ))}
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
                <div className="flex items-start justify-between gap-2">
                  <CardTitle className="text-lg">{listing.title || 'Property listing'}</CardTitle>
                  {listing.listingType === 'SECONDARY' && (
                    <span className="shrink-0 rounded-full bg-violet-100 px-2 py-0.5 text-xs font-medium text-violet-800">
                      Secondary
                    </span>
                  )}
                </div>
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
