import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { TenantRoute } from '@/components/TenantRoute';
import { AppLayout } from '@/components/layout/AppLayout';
import { LeasePage } from '@/pages/LeasePage';
import { LoginPage } from '@/pages/LoginPage';

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          element={
            <TenantRoute>
              <AppLayout />
            </TenantRoute>
          }
        >
          <Route index element={<LeasePage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
