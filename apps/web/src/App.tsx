import { Routes, Route, Navigate } from 'react-router-dom';
import RequireAuth from './components/RequireAuth';
import LoginPage from './pages/LoginPage';
import VoyagePage from './pages/VoyagePage';
import InstitutionPage from './pages/InstitutionPage';
import IslandPage from './pages/IslandPage';
import DocPage from './pages/DocPage';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/" element={<RequireAuth><VoyagePage /></RequireAuth>} />
      <Route path="/institution/:institutionId" element={<RequireAuth><InstitutionPage /></RequireAuth>} />
      <Route path="/island/:islandId" element={<RequireAuth><IslandPage /></RequireAuth>} />
      <Route path="/doc/:docId" element={<RequireAuth><DocPage /></RequireAuth>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
