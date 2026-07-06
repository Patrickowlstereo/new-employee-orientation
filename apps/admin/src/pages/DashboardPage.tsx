import { Button } from 'antd';
import { useAuthStore } from '../stores/authStore';

export default function DashboardPage() {
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  return (
    <div style={{ padding: 24 }}>
      <h1>管理后台 · 概览</h1>
      <p>欢迎，{user?.name}（{user?.username}）</p>
      <p>后台管理功能（用户/机构/小岛/文档/统计）将在后续计划实现。</p>
      <Button onClick={logout}>退出</Button>
    </div>
  );
}
