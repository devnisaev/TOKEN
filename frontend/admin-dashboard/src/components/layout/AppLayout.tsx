import { Outlet } from 'react-router-dom';
import { Header } from '@/components/layout/Header';

export function AppLayout() {
  return (
    <div className="min-h-screen">
      <Header />
      <main className="mx-auto max-w-6xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  );
}
