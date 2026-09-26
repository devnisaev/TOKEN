import { Building2, LogOut, Wallet } from 'lucide-react';
import { Link, NavLink } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { useAuth } from '@/lib/auth';
import { navLinkClass } from '@tokenrealty/shared-ui';

export function Header() {
  const { user, logout } = useAuth();

  return (
    <header className="sticky top-0 z-50 border-b bg-card/95 backdrop-blur supports-[backdrop-filter]:bg-card/80">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4">
        <Link to="/" className="flex items-center gap-2 font-semibold text-foreground">
          <Building2 className="h-6 w-6 text-primary" />
          TokenRealty
        </Link>

        <nav className="hidden items-center gap-1 sm:flex">
          <NavLink to="/" end className={({ isActive }) => navLinkClass(isActive)}>
            Listings
          </NavLink>
          <NavLink to="/orders" className={({ isActive }) => navLinkClass(isActive)}>
            My orders
          </NavLink>
          <NavLink to="/portfolio" className={({ isActive }) => navLinkClass(isActive)}>
            Portfolio
          </NavLink>
          <NavLink to="/dividends" className={({ isActive }) => navLinkClass(isActive)}>
            Dividends
          </NavLink>
        </nav>

        <div className="flex items-center gap-3">
          {user && (
            <span className="hidden text-sm text-muted-foreground md:inline">{user.email}</span>
          )}
          <Button variant="outline" size="sm" onClick={logout}>
            <LogOut className="h-4 w-4" />
            <span className="hidden sm:inline">Sign out</span>
          </Button>
          <Link to="/portfolio">
            <Button size="sm">
              <Wallet className="h-4 w-4" />
              <span className="hidden sm:inline">Portfolio</span>
            </Button>
          </Link>
        </div>
      </div>
    </header>
  );
}
