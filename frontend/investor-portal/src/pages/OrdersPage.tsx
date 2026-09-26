import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ChevronRight, Loader2 } from 'lucide-react';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { formatUsd } from '@/lib/utils';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export function OrdersPage() {
  const { user } = useAuth();

  const { data, isLoading, error } = useQuery({
    queryKey: ['orders', user?.id],
    queryFn: () => api.listOrders(user!.id),
    enabled: !!user?.id,
  });

  if (!user) return null;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">My orders</h1>
        <p className="mt-1 text-muted-foreground">Track buy orders and settlement status</p>
      </div>

      {isLoading && (
        <div className="flex items-center text-muted-foreground">
          <Loader2 className="mr-2 h-5 w-5 animate-spin" />
          Loading…
        </div>
      )}

      {error && (
        <p className="text-destructive">
          {error instanceof Error ? error.message : 'Failed to load orders'}
        </p>
      )}

      {data && data.content.length === 0 && (
        <Card>
          <CardContent className="py-10 text-center text-muted-foreground">
            No orders yet.{' '}
            <Link to="/" className="text-primary underline-offset-4 hover:underline">
              Browse listings
            </Link>
          </CardContent>
        </Card>
      )}

      <ul className="space-y-3">
        {data?.content.map((order) => (
          <li key={order.id}>
            <Link to={`/orders/${order.id}`}>
              <Card className="transition-shadow hover:shadow-md">
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-base font-medium">
                    {order.tokenAmount.toLocaleString()} tokens
                  </CardTitle>
                  <ChevronRight className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent className="flex justify-between text-sm text-muted-foreground">
                  <span>{formatUsd(order.totalPriceUsd)}</span>
                  <span className="font-medium text-foreground">{order.status}</span>
                </CardContent>
              </Card>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}
