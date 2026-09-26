import type { ReactNode } from 'react';
import { cn } from '../../lib/utils';

export interface AppShellProps {
  header: ReactNode;
  children: ReactNode;
  maxWidth?: 'lg' | '6xl';
}

export function AppShell({ header, children, maxWidth = '6xl' }: AppShellProps) {
  return (
    <div className="min-h-screen">
      {header}
      <main
        className={cn(
          'mx-auto px-4 py-8',
          maxWidth === 'lg' ? 'max-w-lg' : 'max-w-6xl',
        )}
      >
        {children}
      </main>
    </div>
  );
}
