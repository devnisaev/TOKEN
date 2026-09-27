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
import { FlatTokenizePage } from '@/pages/FlatTokenizePage';
import { LoginPage } from '@/pages/LoginPage';
import { MaintenanceTicketsPage } from '@/pages/MaintenanceTicketsPage';
import { GovernanceAdminPage } from '@/pages/GovernanceAdminPage';
import { IntegrationsPage } from '@/pages/IntegrationsPage';
import { IntegrationsDeliveriesPage } from '@/pages/IntegrationsDeliveriesPage';
import { ComplianceReportsPage } from '@/pages/ComplianceReportsPage';
import { OrderDetailPage } from '@/pages/OrderDetailPage';
import { OrdersPage } from '@/pages/OrdersPage';
import { StandaloneAssetFormPage } from '@/pages/StandaloneAssetFormPage';
import { PoolsPage } from '@/pages/PoolsPage';
import { OtcPage } from '@/pages/OtcPage';
import { IndicesManagePage } from '@/pages/IndicesManagePage';
import { SurveillancePage } from '@/pages/SurveillancePage';
import { EsgPage } from '@/pages/EsgPage';
import { OperatorKpisPage } from '@/pages/OperatorKpisPage';
import { OperatorAlertsPage } from '@/pages/OperatorAlertsPage';
import { BuildingHealthPage } from '@/pages/BuildingHealthPage';
import { OperationsReportsPage } from '@/pages/OperationsReportsPage';

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
          <Route path="buildings/standalone/new" element={<StandaloneAssetFormPage />} />
          <Route path="buildings/:buildingId/edit" element={<BuildingFormPage />} />
          <Route path="buildings/:buildingId/flats/new" element={<FlatFormPage />} />
          <Route path="buildings/:buildingId/flats/:flatId/edit" element={<FlatFormPage />} />
          <Route path="buildings/:buildingId/flats/:flatId/tokenize" element={<FlatTokenizePage />} />
          <Route path="buildings/:buildingId" element={<BuildingDetailPage />} />
          <Route path="orders" element={<OrdersPage />} />
          <Route path="pools" element={<PoolsPage />} />
          <Route path="otc" element={<OtcPage />} />
          <Route path="indices/manage" element={<IndicesManagePage />} />
          <Route path="surveillance" element={<SurveillancePage />} />
          <Route path="esg" element={<EsgPage />} />
          <Route path="operator-kpis" element={<OperatorKpisPage />} />
          <Route path="operator-alerts" element={<OperatorAlertsPage />} />
          <Route path="building-health" element={<BuildingHealthPage />} />
          <Route path="orders/:orderId" element={<OrderDetailPage />} />
          <Route path="compliance" element={<CompliancePage />} />
          <Route path="document-reviews" element={<DocumentReviewsPage />} />
          <Route path="maintenance" element={<MaintenanceTicketsPage />} />
          <Route path="governance" element={<GovernanceAdminPage />} />
          <Route path="integrations" element={<IntegrationsPage />} />
          <Route path="integrations/deliveries" element={<IntegrationsDeliveriesPage />} />
          <Route path="reports/compliance" element={<ComplianceReportsPage />} />
          <Route path="reports/operations" element={<OperationsReportsPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
