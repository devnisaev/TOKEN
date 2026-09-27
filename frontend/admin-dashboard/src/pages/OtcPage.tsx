import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { formatUsd } from '@/lib/utils';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export function OtcPage() {
  const rfqQuery = useQuery({
    queryKey: ['open-rfq'],
    queryFn: () => api.listOpenRfq(),
  });

  return (
    <div className="mx-auto max-w-5xl space-y-6 p-6">
      <h1 className="text-2xl font-semibold">OTC / RFQ desk</h1>
      <p className="text-muted-foreground">Block trades ($500k+ notional) awaiting quotes</p>

      {rfqQuery.isLoading && <p className="text-muted-foreground">Loading RFQ queue…</p>}
      <div className="space-y-3">
        {rfqQuery.data?.content.map((rfq) => (
          <Card key={rfq.id}>
            <CardHeader className="pb-2">
              <CardTitle className="text-base">
                {rfq.side} · {rfq.tokenAmount.toLocaleString()} tokens · {formatUsd(rfq.notionalUsd)}
              </CardTitle>
            </CardHeader>
            <CardContent className="text-sm text-muted-foreground">
              Contract {rfq.contractId.slice(0, 8)}… · {rfq.liquidityTier} · expires {new Date(rfq.expiresAt).toLocaleString()}
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
