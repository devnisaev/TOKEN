import { Outlet } from 'react-router-dom';
import { AppShell } from '@tokenrealty/shared-ui';
import { Header } from '@/components/layout/Header';

export function AppLayout() {
  return (
    <AppShell header={<Header />}>
      <Outlet />
    </AppShell>
  );
}
