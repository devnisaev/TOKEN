import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AppLayout } from '@/components/layout/AppLayout';
import { ProtectedRoute } from '@/components/ProtectedRoute';
import { ListingDetailPage } from '@/pages/ListingDetailPage';
import { ListingsPage } from '@/pages/ListingsPage';
import { LoginPage } from '@/pages/LoginPage';
import { PortfolioPage } from '@/pages/PortfolioPage';

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
          <Route path="portfolio" element={<PortfolioPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
