import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AdminRoute } from '@/components/AdminRoute';
import { AppLayout } from '@/components/layout/AppLayout';
import { BuildingsPage } from '@/pages/BuildingsPage';
import { CompliancePage } from '@/pages/CompliancePage';
import { DashboardPage } from '@/pages/DashboardPage';
import { LoginPage } from '@/pages/LoginPage';

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          element={
            <AdminRoute>
              <AppLayout />
            </AdminRoute>
          }
        >
          <Route index element={<DashboardPage />} />
          <Route path="buildings" element={<BuildingsPage />} />
          <Route path="compliance" element={<CompliancePage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
