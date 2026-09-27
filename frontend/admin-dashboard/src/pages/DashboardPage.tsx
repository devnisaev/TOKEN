import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { AlertTriangle, Building2, FileCheck, ShieldCheck, Wrench } from 'lucide-react';
import { api } from '@/lib/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export function DashboardPage() {
  const buildings = useQuery({ queryKey: ['buildings'], queryFn: () => api.listBuildings() });
  const compliance = useQuery({ queryKey: ['compliance'], queryFn: () => api.listCompliance() });
  const docReviews = useQuery({
    queryKey: ['document-reviews', 'pending'],
    queryFn: () => api.listPendingDocumentReviews(),
  });
  const operatorKpis = useQuery({
    queryKey: ['operator-kpis'],
    queryFn: () => api.getOperatorKpis(),
  });

  const pendingKyc =
    compliance.data?.content.filter((r) => r.status === 'PENDING').length ?? '—';
  const pendingDocs = docReviews.data?.length ?? '—';

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Dashboard</h1>
        <p className="text-muted-foreground">TokenRealty platform overview</p>
      </div>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
        <Link to="/buildings">
          <Card className="transition-shadow hover:shadow-md">
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium">Buildings</CardTitle>
              <Building2 className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">{buildings.data?.totalElements ?? '—'}</p>
              <p className="text-xs text-muted-foreground">Registered properties</p>
            </CardContent>
          </Card>
        </Link>
        <Link to="/compliance">
          <Card className="transition-shadow hover:shadow-md">
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium">KYC pending</CardTitle>
              <ShieldCheck className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">{pendingKyc}</p>
              <p className="text-xs text-muted-foreground">Awaiting review</p>
            </CardContent>
          </Card>
        </Link>
        <Link to="/document-reviews">
          <Card className="transition-shadow hover:shadow-md">
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium">Documents pending</CardTitle>
              <FileCheck className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">{pendingDocs}</p>
              <p className="text-xs text-muted-foreground">Awaiting review</p>
            </CardContent>
          </Card>
        </Link>
        <Link to="/operator-alerts">
          <Card className="transition-shadow hover:shadow-md">
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium">Open alerts</CardTitle>
              <AlertTriangle className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">
                {operatorKpis.data?.openOperatorAlertCount ?? '—'}
              </p>
              <p className="text-xs text-muted-foreground">Operator signals</p>
            </CardContent>
          </Card>
        </Link>
        <Link to="/maintenance">
          <Card className="transition-shadow hover:shadow-md">
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium">Maintenance open</CardTitle>
              <Wrench className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">
                {operatorKpis.data?.openMaintenanceTicketCount ?? '—'}
              </p>
              <p className="text-xs text-muted-foreground">Active tickets</p>
            </CardContent>
          </Card>
        </Link>
      </div>
    </div>
  );
}
