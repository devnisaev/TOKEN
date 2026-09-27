import { useQuery } from '@tanstack/react-query';
import { Loader2 } from 'lucide-react';
import { api } from '@/lib/api';
import { formatUsd } from '@/lib/utils';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export function ComplianceReportsPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['compliance-report'],
    queryFn: () => api.getComplianceReport(),
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Compliance reports</h1>
        <p className="text-muted-foreground">Tax withholding summaries and surveillance alerts</p>
      </div>

      {isLoading && (
        <div className="flex items-center text-muted-foreground">
          <Loader2 className="mr-2 h-5 w-5 animate-spin" />
          Loading…
        </div>
      )}

      {error && (
        <p className="text-destructive">{error instanceof Error ? error.message : 'Failed to load'}</p>
      )}

      {data && (
        <>
          <div className="grid gap-4 sm:grid-cols-2">
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">Tax summaries</CardTitle>
              </CardHeader>
              <CardContent className="text-2xl font-bold">{data.taxSummaryTotal}</CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">Surveillance alerts</CardTitle>
              </CardHeader>
              <CardContent className="text-2xl font-bold">{data.surveillanceAlertTotal}</CardContent>
            </Card>
          </div>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Tax withholding</CardTitle>
            </CardHeader>
            <CardContent className="overflow-x-auto p-0">
              <table className="w-full text-sm">
                <thead className="border-b bg-muted/50">
                  <tr>
                    <th className="px-4 py-3 text-left font-medium">Investor</th>
                    <th className="px-4 py-3 text-left font-medium">Gross</th>
                    <th className="px-4 py-3 text-left font-medium">Withholding</th>
                    <th className="px-4 py-3 text-left font-medium">Net</th>
                  </tr>
                </thead>
                <tbody>
                  {data.taxSummaries.map((row) => (
                    <tr key={row.id} className="border-b last:border-0">
                      <td className="px-4 py-3 font-mono text-xs">{row.recipientInvestorId}</td>
                      <td className="px-4 py-3">{formatUsd(row.grossAmountUsd)}</td>
                      <td className="px-4 py-3">{formatUsd(row.withholdingAmountUsd)}</td>
                      <td className="px-4 py-3">{formatUsd(row.netAmountUsd)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Surveillance alerts</CardTitle>
            </CardHeader>
            <CardContent className="overflow-x-auto p-0">
              <table className="w-full text-sm">
                <thead className="border-b bg-muted/50">
                  <tr>
                    <th className="px-4 py-3 text-left font-medium">Type</th>
                    <th className="px-4 py-3 text-left font-medium">Order</th>
                    <th className="px-4 py-3 text-left font-medium">Detected</th>
                  </tr>
                </thead>
                <tbody>
                  {data.surveillanceAlerts.map((row) => (
                    <tr key={row.id} className="border-b last:border-0">
                      <td className="px-4 py-3">{row.alertType}</td>
                      <td className="px-4 py-3 font-mono text-xs">{row.orderId}</td>
                      <td className="px-4 py-3">{new Date(row.detectedAt).toLocaleString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </CardContent>
          </Card>
        </>
      )}
    </div>
  );
}
