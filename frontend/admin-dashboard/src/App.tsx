import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AdminRoute } from '@/components/AdminRoute';
import { AppLayout } from '@/components/layout/AppLayout';
import { BuildingDetailPage } from '@/pages/BuildingDetailPage';
import { BuildingFormPage } from '@/pages/BuildingFormPage';
import { BuildingsPage } from '@/pages/BuildingsPage';
import { CompliancePage } from '@/pages/CompliancePage';
import { DashboardPage } from '@/pages/DashboardPage';
import { DocumentReviewsPage } from '@/pages/DocumentReviewsPage';
import { FlatFormPage } from '@/pages/FlatFormPage';
import { LoginPage } from '@/pages/LoginPage';
import { OrderDetailPage } from '@/pages/OrderDetailPage';
import { OrdersPage } from '@/pages/OrdersPage';

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
          <Route path="buildings/new" element={<BuildingFormPage />} />
          <Route path="buildings/:buildingId/edit" element={<BuildingFormPage />} />
          <Route path="buildings/:buildingId/flats/new" element={<FlatFormPage />} />
          <Route path="buildings/:buildingId/flats/:flatId/edit" element={<FlatFormPage />} />
          <Route path="buildings/:buildingId" element={<BuildingDetailPage />} />
          <Route path="orders" element={<OrdersPage />} />
          <Route path="orders/:orderId" element={<OrderDetailPage />} />
          <Route path="compliance" element={<CompliancePage />} />
          <Route path="document-reviews" element={<DocumentReviewsPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
