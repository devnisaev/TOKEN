import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/lib/auth';

export function TenantRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isTenant, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center text-muted-foreground">
        Loading…
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  if (!isTenant) {
    return (
      <div className="mx-auto max-w-md py-20 text-center">
        <p className="text-destructive">Access denied — tenant role required.</p>
      </div>
    );
  }

  return children;
}
