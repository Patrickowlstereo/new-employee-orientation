import { Card, Typography } from 'antd';
import { Link } from 'react-router-dom';

const { Title, Paragraph } = Typography;

export default function DashboardPage() {
  return (
    <Card>
      <Title level={4}>管理后台 · 概览</Title>
      <Paragraph>
        阶段 4 已开放「文档管理」：管理员可在后台直接上传/替换/删除学习文件（文档、图片、视频、音频、压缩包），
        无需找开发改代码。
      </Paragraph>
      <Paragraph type="secondary">
        后续阶段将补充：用户/机构/小岛维护、全员学习完成率统计。
      </Paragraph>
      <Link to="/admin/docs">→ 前往文档管理</Link>
    </Card>
  );
}
