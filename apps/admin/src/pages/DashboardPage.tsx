import { Card, Typography, Space } from 'antd';
import { Link } from 'react-router-dom';

const { Title, Paragraph } = Typography;

export default function DashboardPage() {
  return (
    <Card>
      <Title level={4}>管理后台 · 概览</Title>
      <Paragraph>
        管理员可在此自助维护学习内容、查看全员学习情况，无需联系技术人员：
      </Paragraph>
      <Space direction="vertical">
        <div><Link to="/admin/docs">文档管理</Link> — 上传/替换/删除学习文件，新增/编辑/停用文档</div>
        <div><Link to="/admin/institutions">机构管理</Link> — 维护机构（编码/名称/排序）</div>
        <div><Link to="/admin/islands">小岛管理</Link> — 维护各机构下的知识小岛</div>
        <div><Link to="/admin/stats">统计报表</Link> — 全员学习完成率、各小岛完成率、学员明细</div>
      </Space>
      <Paragraph type="secondary" style={{ marginTop: 16 }}>
        如需开通管理员账号或调整学员账号，请联系系统管理员。
      </Paragraph>
    </Card>
  );
}
