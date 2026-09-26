import { cn } from '../../lib/utils';
import type { ReactNode } from 'react';

export interface NavItem {
  to: string;
  label: string;
  end?: boolean;
}

export interface ShellHeaderProps {
  brand: ReactNode;
  navItems?: NavItem[];
  actions?: ReactNode;
  renderNavLink: (item: NavItem, className: string) => ReactNode;
}

export function navLinkClass(isActive: boolean) {
  return cn(
    'rounded-md px-3 py-2 text-sm font-medium transition-colors',
    isActive ? 'bg-accent text-accent-foreground' : 'text-muted-foreground hover:text-foreground',
  );
}

export function ShellHeader({ brand, navItems = [], actions, renderNavLink }: ShellHeaderProps) {
  return (
    <header className="sticky top-0 z-50 border-b bg-card/95 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4">
        {brand}
        {navItems.length > 0 && (
          <nav className="flex items-center gap-1">
            {navItems.map((item) =>
              renderNavLink(item, navLinkClass(false)),
            )}
          </nav>
        )}
        {actions}
      </div>
    </header>
  );
}
