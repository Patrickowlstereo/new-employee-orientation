import { useEffect, useState } from 'react';
import { Card, Table, Statistic, Row, Col, Progress, message, Typography, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { fetchStatsOverview, fetchUserStats } from '../api/stats';
import type { StatsOverview, UserStats, IslandCompletion } from '@gmnl/shared';

const { Title } = Typography;

export default function StatsPage() {
  const [overview, setOverview] = useState<StatsOverview | null>(null);
  const [users, setUsers] = useState<UserStats[]>([]);
  const [loading, setLoading] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const [o, u] = await Promise.all([fetchStatsOverview(), fetchUserStats()]);
      setOverview(o);
      setUsers(u);
    } catch {
      message.error('加载失败');
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => { load(); }, []);

  const islandColumns: ColumnsType<IslandCompletion> = [
    { title: '小岛', dataIndex: 'islandName', key: 'islandName' },
    { title: '机构', dataIndex: 'institutionName', key: 'institutionName', width: 100 },
    {
      title: '完成率', key: 'pct', width: 220,
      render: (_, r) => <Progress percent={r.completionPct} size="small" />,
    },
    {
      title: '已完成 / 学员数', key: 'count', width: 140,
      render: (_, r) => `${r.completedUsers} / ${r.totalLearners}`,
    },
  ];

  const userColumns: ColumnsType<UserStats> = [
    { title: '姓名', dataIndex: 'name', key: 'name' },
    { title: '账号', dataIndex: 'username', key: 'username', width: 140 },
    {
      title: '完成率', key: 'pct', width: 220,
      render: (_, r) => <Progress percent={r.completionPct} size="small" status={r.completionPct === 100 ? 'success' : 'active'} />,
    },
    {
      title: '必修完成', key: 'req', width: 120,
      render: (_, r) => `${r.requiredCompleted} / ${r.requiredTotal}`,
    },
    {
      title: '小岛完成', key: 'islands', width: 120,
      render: (_, r) => `${r.islandsCompleted} / ${r.islandsTotal}`,
    },
    {
      title: '最后学习', key: 'last', width: 170,
      render: (_, r) => r.lastReadAt ? new Date(r.lastReadAt).toLocaleString('zh-CN') : <Tag>未开始</Tag>,
    },
  ];

  return (
    <div>
      <Card loading={loading && !overview} style={{ marginBottom: 16 }}>
        <Title level={4} style={{ marginTop: 0 }}>学习统计概览</Title>
        <Row gutter={24}>
          <Col span={6}><Statistic title="总账号数" value={overview?.totalUsers ?? 0} /></Col>
          <Col span={6}><Statistic title="学员数" value={overview?.totalLearners ?? 0} /></Col>
          <Col span={6}><Statistic title="全部必修完成人数" value={overview?.completedAllRequired ?? 0} /></Col>
          <Col span={6}><Statistic title="平均完成率" value={overview?.avgCompletionPct ?? 0} suffix="%" /></Col>
        </Row>
      </Card>

      <Card title="各小岛完成率" style={{ marginBottom: 16 }} loading={loading}>
        <Table
          rowKey="islandId"
          size="small"
          columns={islandColumns}
          dataSource={overview?.islands ?? []}
          pagination={false}
        />
      </Card>

      <Card title="学员学习明细" loading={loading}>
        <Table
          rowKey="userId"
          size="small"
          columns={userColumns}
          dataSource={users}
          pagination={{ pageSize: 20, showSizeChanger: false }}
        />
      </Card>
    </div>
  );
}
