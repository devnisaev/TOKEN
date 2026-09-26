import { Building2, LogOut } from 'lucide-react';
import { Link, NavLink } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { useAuth } from '@/lib/auth';
import { navLinkClass } from '@tokenrealty/shared-ui';

export function Header() {
  const { user, logout } = useAuth();

  return (
    <header className="sticky top-0 z-50 border-b bg-card/95 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4">
        <Link to="/" className="flex items-center gap-2 font-semibold">
          <Building2 className="h-6 w-6 text-primary" />
          TokenRealty Admin
        </Link>
        <nav className="flex items-center gap-1">
          <NavLink to="/" end className={({ isActive }) => navLinkClass(isActive)}>
            Dashboard
          </NavLink>
          <NavLink to="/buildings" className={({ isActive }) => navLinkClass(isActive)}>
            Buildings
          </NavLink>
          <NavLink to="/orders" className={({ isActive }) => navLinkClass(isActive)}>
            Orders
          </NavLink>
          <NavLink to="/compliance" className={({ isActive }) => navLinkClass(isActive)}>
            KYC
          </NavLink>
          <NavLink to="/document-reviews" className={({ isActive }) => navLinkClass(isActive)}>
            Documents
          </NavLink>
        </nav>
        <div className="flex items-center gap-3">
          {user && (
            <span className="hidden text-sm text-muted-foreground md:inline">
              {user.email} · {user.role}
            </span>
          )}
          <Button variant="outline" size="sm" onClick={logout}>
            <LogOut className="h-4 w-4" />
            Sign out
          </Button>
        </div>
      </div>
    </header>
  );
}
