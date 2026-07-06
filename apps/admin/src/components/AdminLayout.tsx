import { Layout, Menu, Button, Space, Typography } from 'antd';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';

const { Sider, Header, Content } = Layout;
const { Text } = Typography;

/** 后台整体布局：左侧菜单 + 顶部用户/退出，子路由经 Outlet 渲染。 */
export default function AdminLayout() {
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  const navigate = useNavigate();
  const location = useLocation();

  const items = [
    { key: '/admin/', label: '概览' },
    { key: '/admin/docs', label: '文档管理' },
    { key: '/admin/institutions', label: '机构管理' },
    { key: '/admin/islands', label: '小岛管理' },
    { key: '/admin/stats', label: '统计报表' },
  ];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider breakpoint="lg" collapsedWidth={0}>
        <div style={{ color: '#fff', padding: 16, textAlign: 'center', fontWeight: 600 }}>
          新人航行 · 后台
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={items}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <Layout>
        <Header style={{ background: '#fff', padding: '0 24px', display: 'flex', justifyContent: 'flex-end' }}>
          <Space>
            <Text>欢迎，{user?.name}（{user?.username}）</Text>
            <Button onClick={logout}>退出</Button>
          </Space>
        </Header>
        <Content style={{ padding: 24, background: '#f5f5f5' }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
