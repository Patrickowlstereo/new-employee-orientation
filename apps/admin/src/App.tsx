import { useEffect } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from './stores/authStore';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import DocsManagePage from './pages/DocsManagePage';
import AdminLayout from './components/AdminLayout';

function RequireAdmin({ children }: { children: React.ReactNode }) {
  const token = useAuthStore((s) => s.token);
  const user = useAuthStore((s) => s.user);
  const fetchMe = useAuthStore((s) => s.fetchMe);
  useEffect(() => { if (token && !user) fetchMe(); }, [token, user, fetchMe]);
  if (!token) return <Navigate to="/admin/login" replace />;
  return <>{children}</>;
}

export default function App() {
  return (
    <Routes>
      <Route path="/admin/login" element={<LoginPage />} />
      <Route path="/admin/" element={<RequireAdmin><AdminLayout /></RequireAdmin>}>
        <Route index element={<DashboardPage />} />
        <Route path="docs" element={<DocsManagePage />} />
      </Route>
      <Route path="*" element={<Navigate to="/admin/" replace />} />
    </Routes>
  );
}
