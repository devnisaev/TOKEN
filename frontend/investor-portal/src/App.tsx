import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AppLayout } from '@/components/layout/AppLayout';
import { ProtectedRoute } from '@/components/ProtectedRoute';
import { ListingDetailPage } from '@/pages/ListingDetailPage';
import { ListingsPage } from '@/pages/ListingsPage';
import { LoginPage } from '@/pages/LoginPage';
import { OrderStatusPage } from '@/pages/OrderStatusPage';
import { OrdersPage } from '@/pages/OrdersPage';
import { DividendsPage } from '@/pages/DividendsPage';
import { PortfolioPage } from '@/pages/PortfolioPage';
import { SellTokensPage } from '@/pages/SellTokensPage';

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
          <Route path="dividends" element={<DividendsPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
