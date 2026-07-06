import { Routes, Route, Navigate } from 'react-router-dom';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<div>登录页占位</div>} />
      <Route path="/" element={<div>航行首页占位</div>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
