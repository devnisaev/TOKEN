import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AppLayout } from '@/components/layout/AppLayout';
import { ProtectedRoute } from '@/components/ProtectedRoute';
import { ListingDetailPage } from '@/pages/ListingDetailPage';
import { ListingsPage } from '@/pages/ListingsPage';
import { LoginPage } from '@/pages/LoginPage';
import { OrderStatusPage } from '@/pages/OrderStatusPage';
import { OrdersPage } from '@/pages/OrdersPage';
import { DividendsPage } from '@/pages/DividendsPage';
import { GovernanceDetailPage } from '@/pages/GovernanceDetailPage';
import { GovernancePage } from '@/pages/GovernancePage';
import { NotificationPreferencesPage } from '@/pages/NotificationPreferencesPage';
import { PortfolioPage } from '@/pages/PortfolioPage';
import { SellTokensPage } from '@/pages/SellTokensPage';
import { ExchangePage } from '@/pages/ExchangePage';
import { PoolSwapPage } from '@/pages/PoolSwapPage';
import { IndicesPage } from '@/pages/IndicesPage';
import { LendingPage } from '@/pages/LendingPage';
import { SustainabilityPage } from '@/pages/SustainabilityPage';

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          element={
            <ProtectedRoute>
              <AppLayout />
            </ProtectedRoute>
          }
        >
          <Route index element={<ListingsPage />} />
          <Route path="listings/:listingId" element={<ListingDetailPage />} />
          <Route path="orders" element={<OrdersPage />} />
          <Route path="orders/:orderId" element={<OrderStatusPage />} />
          <Route path="portfolio" element={<PortfolioPage />} />
          <Route path="portfolio/sell/:contractId" element={<SellTokensPage />} />
          <Route path="exchange/:contractId" element={<ExchangePage />} />
          <Route path="exchange/:contractId/swap" element={<PoolSwapPage />} />
          <Route path="indices" element={<IndicesPage />} />
          <Route path="lending" element={<LendingPage />} />
          <Route path="assets/:flatId/sustainability" element={<SustainabilityPage />} />
          <Route path="dividends" element={<DividendsPage />} />
          <Route path="governance" element={<GovernancePage />} />
          <Route path="governance/:proposalId" element={<GovernanceDetailPage />} />
          <Route path="settings/notifications" element={<NotificationPreferencesPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
