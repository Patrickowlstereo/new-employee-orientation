import { Card, Typography, Space } from 'antd';
import { Link } from 'react-router-dom';

const { Title, Paragraph } = Typography;

export default function DashboardPage() {
  return (
    <Card>
      <Title level={4}>管理后台 · 概览</Title>
      <Paragraph>
        阶段 4–5 已开放以下后台能力，管理员可自助维护内容、查看全员学习情况，无需找开发改代码：
      </Paragraph>
      <Space direction="vertical">
        <div><Link to="/admin/docs">文档管理</Link> — 上传/替换/删除学习文件，新增/编辑/停用文档</div>
        <div><Link to="/admin/institutions">机构管理</Link> — 维护机构（编码/名称/排序）</div>
        <div><Link to="/admin/islands">小岛管理</Link> — 维护各机构下的知识小岛</div>
        <div><Link to="/admin/stats">统计报表</Link> — 全员学习完成率、各小岛完成率、学员明细</div>
      </Space>
      <Paragraph type="secondary" style={{ marginTop: 16 }}>
        后续阶段：员工账号管理（创建/停用/重置）、航海视觉迁移。
      </Paragraph>
    </Card>
  );
}
