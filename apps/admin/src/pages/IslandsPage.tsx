import { useEffect, useMemo, useState } from 'react';
import { Card, Table, Button, Space, Select, Modal, Form, Input, InputNumber, Popconfirm, message, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { fetchInstitutionsWithIslands, createIsland, updateIsland, deleteIsland } from '../api/content';
import type { Institution, Island, IslandUpsertRequest } from '@gmnl/shared';

const { Title } = Typography;

type Row = Island & { institutionName: string };

export default function IslandsPage() {
  const [institutions, setInstitutions] = useState<Institution[]>([]);
  const [loading, setLoading] = useState(false);
  const [instFilter, setInstFilter] = useState<number | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Row | null>(null);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm<IslandUpsertRequest>();

  const load = async () => {
    setLoading(true);
    try { setInstitutions(await fetchInstitutionsWithIslands()); }
    catch { message.error('加载失败'); }
    finally { setLoading(false); }
  };
  useEffect(() => { load(); }, []);

  const instName = useMemo(() => {
    const m = new Map<number, string>();
    institutions.forEach((i) => m.set(i.id, i.name));
    return m;
  }, [institutions]);

  const rows: Row[] = useMemo(() =>
    institutions.flatMap((i) => i.islands.map((is) => ({ ...is, institutionName: i.name }))),
    [institutions]);
  const filtered = instFilter ? rows.filter((r) => r.institutionId === instFilter) : rows;

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ order: 0, institutionId: instFilter ?? undefined });
    setModalOpen(true);
  };
  const openEdit = (r: Row) => {
    setEditing(r);
    form.setFieldsValue({ key: r.key, name: r.name, order: r.order, institutionId: r.institutionId });
    setModalOpen(true);
  };

  const submit = async (v: IslandUpsertRequest) => {
    setSaving(true);
    try {
      const req = { ...v, order: v.order ?? null };
      if (editing) await updateIsland(editing.id, req);
      else await createIsland(req);
      message.success(editing ? '已更新' : '已创建');
      setModalOpen(false);
      await load();
    } catch (e: any) {
      message.error(e?.response?.data?.message || '操作失败');
    } finally { setSaving(false); }
  };

  const remove = async (r: Row) => {
    try { await deleteIsland(r.id); message.success('已删除'); await load(); }
    catch (e: any) { message.error(e?.response?.data?.message || '删除失败'); }
  };

  const columns: ColumnsType<Row> = [
    { title: '小岛名称', dataIndex: 'name', key: 'name' },
    { title: '编码', dataIndex: 'key', key: 'key', width: 140 },
    { title: '所属机构', key: 'inst', width: 100, render: (_, r) => instName.get(r.institutionId) ?? r.institutionId },
    { title: '排序', dataIndex: 'order', key: 'order', width: 80 },
    {
      title: '操作', key: 'actions', width: 160,
      render: (_, r) => (
        <Space>
          <Button size="small" onClick={() => openEdit(r)}>编辑</Button>
          <Popconfirm title="删除该小岛？" description="仅当小岛下无文档时可删除。" onConfirm={() => remove(r)}>
            <Button size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <Card title={<Title level={4} style={{ margin: 0 }}>小岛管理</Title>} extra={<Button type="primary" onClick={openCreate}>新增小岛</Button>}>
      <Space style={{ marginBottom: 16 }}>
        <Select
          style={{ width: 180 }}
          placeholder="按机构筛选"
          allowClear
          value={instFilter ?? undefined}
          onChange={(v) => setInstFilter(v ?? null)}
          options={institutions.map((i) => ({ value: i.id, label: i.name }))}
        />
      </Space>
      <Table rowKey="id" loading={loading} columns={columns} dataSource={filtered} pagination={false} />
      <Modal
        open={modalOpen}
        title={editing ? '编辑小岛' : '新增小岛'}
        onCancel={() => setModalOpen(false)}
        confirmLoading={saving}
        onOk={() => form.submit()}
      >
        <Form form={form} layout="vertical" onFinish={submit}>
          <Form.Item name="institutionId" label="所属机构" rules={[{ required: true, message: '请选择机构' }]}>
            <Select options={institutions.map((i) => ({ value: i.id, label: i.name }))} placeholder="选择机构" />
          </Form.Item>
          <Form.Item name="key" label="编码" rules={[{ required: true, message: '请输入编码' }, { max: 16 }]}>
            <Input placeholder="如 about_BJ" />
          </Form.Item>
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input placeholder="如 关于公司" />
          </Form.Item>
          <Form.Item name="order" label="排序">
            <InputNumber min={0} />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
}
