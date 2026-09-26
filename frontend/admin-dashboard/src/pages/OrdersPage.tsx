import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Loader2 } from 'lucide-react';
import { api } from '@/lib/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export function OrdersPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['admin-orders'],
    queryFn: () => api.listOrders(),
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Orders</h1>
        <p className="text-muted-foreground">Marketplace buy/sell order monitoring</p>
      </div>

      {isLoading && (
        <div className="flex items-center text-muted-foreground">
          <Loader2 className="mr-2 h-5 w-5 animate-spin" />
          Loading orders…
        </div>
      )}

      {error && (
        <p className="text-destructive">{error instanceof Error ? error.message : 'Failed to load'}</p>
      )}

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Recent orders ({data?.totalElements ?? 0})</CardTitle>
        </CardHeader>
        <CardContent className="overflow-x-auto p-0">
          <table className="w-full text-sm">
            <thead className="border-b bg-muted/50">
              <tr>
                <th className="px-4 py-3 text-left font-medium">Order ID</th>
                <th className="px-4 py-3 text-left font-medium">Buyer</th>
                <th className="px-4 py-3 text-left font-medium">Tokens</th>
                <th className="px-4 py-3 text-left font-medium">Total USD</th>
                <th className="px-4 py-3 text-left font-medium">Status</th>
              </tr>
            </thead>
            <tbody>
              {data?.content.map((order) => (
                <tr key={order.id} className="border-b last:border-0">
                  <td className="px-4 py-3 font-mono text-xs">
                    <Link to={`/orders/${order.id}`} className="text-primary hover:underline">
                      {order.id?.slice(0, 8)}…
                    </Link>
                  </td>
                  <td className="px-4 py-3 font-mono text-xs">{order.buyerId?.slice(0, 8)}…</td>
                  <td className="px-4 py-3">{order.tokenAmount}</td>
                  <td className="px-4 py-3">${order.totalPriceUsd}</td>
                  <td className="px-4 py-3">
                    <span className="rounded-md bg-secondary px-2 py-0.5 text-xs">{order.status}</span>
                  </td>
                </tr>
              ))}
              {data?.content.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-4 py-8 text-center text-muted-foreground">
                    No orders yet.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </CardContent>
      </Card>
    </div>
  );
}
