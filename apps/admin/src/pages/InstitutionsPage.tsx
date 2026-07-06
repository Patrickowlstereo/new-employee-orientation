import { useEffect, useState } from 'react';
import { Card, Table, Button, Space, Modal, Form, Input, InputNumber, Popconfirm, message, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { fetchInstitutionsWithIslands, createInstitution, updateInstitution, deleteInstitution } from '../api/content';
import type { Institution, InstitutionUpsertRequest } from '@gmnl/shared';

const { Title } = Typography;

export default function InstitutionsPage() {
  const [institutions, setInstitutions] = useState<Institution[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Institution | null>(null);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm<InstitutionUpsertRequest>();

  const load = async () => {
    setLoading(true);
    try { setInstitutions(await fetchInstitutionsWithIslands()); }
    catch { message.error('加载失败'); }
    finally { setLoading(false); }
  };
  useEffect(() => { load(); }, []);

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ order: 0 });
    setModalOpen(true);
  };
  const openEdit = (inst: Institution) => {
    setEditing(inst);
    form.setFieldsValue({ key: inst.key, name: inst.name, order: inst.order });
    setModalOpen(true);
  };

  const submit = async (v: InstitutionUpsertRequest) => {
    setSaving(true);
    try {
      const req = { ...v, order: v.order ?? null };
      if (editing) await updateInstitution(editing.id, req);
      else await createInstitution(req);
      message.success(editing ? '已更新' : '已创建');
      setModalOpen(false);
      await load();
    } catch (e: any) {
      message.error(e?.response?.data?.message || '操作失败');
    } finally { setSaving(false); }
  };

  const remove = async (inst: Institution) => {
    try { await deleteInstitution(inst.id); message.success('已删除'); await load(); }
    catch (e: any) { message.error(e?.response?.data?.message || '删除失败'); }
  };

  const columns: ColumnsType<Institution> = [
    { title: '机构名称', dataIndex: 'name', key: 'name' },
    { title: '编码', dataIndex: 'key', key: 'key', width: 100 },
    { title: '排序', dataIndex: 'order', key: 'order', width: 80 },
    { title: '小岛数', key: 'islands', width: 90, render: (_, r) => r.islands?.length ?? 0 },
    {
      title: '操作', key: 'actions', width: 160,
      render: (_, r) => (
        <Space>
          <Button size="small" onClick={() => openEdit(r)}>编辑</Button>
          <Popconfirm title="删除该机构？" description="仅当机构下无小岛时可删除。" onConfirm={() => remove(r)}>
            <Button size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <Card title={<Title level={4} style={{ margin: 0 }}>机构管理</Title>} extra={<Button type="primary" onClick={openCreate}>新增机构</Button>}>
      <Table rowKey="id" loading={loading} columns={columns} dataSource={institutions} pagination={false} />
      <Modal
        open={modalOpen}
        title={editing ? '编辑机构' : '新增机构'}
        onCancel={() => setModalOpen(false)}
        confirmLoading={saving}
        onOk={() => form.submit()}
      >
        <Form form={form} layout="vertical" onFinish={submit}>
          <Form.Item name="key" label="编码" rules={[{ required: true, message: '请输入编码' }, { max: 8 }]}>
            <Input placeholder="如 BJ" />
          </Form.Item>
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input placeholder="如 北京" />
          </Form.Item>
          <Form.Item name="order" label="排序">
            <InputNumber min={0} />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
}
