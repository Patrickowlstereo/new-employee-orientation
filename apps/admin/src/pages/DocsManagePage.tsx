import { useEffect, useMemo, useState } from 'react';
import { Card, Table, Select, Button, Space, Tag, Modal, message, Popconfirm, Typography, Input, Form, InputNumber, Switch } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  fetchAdminDocs,
  fetchInstitutionsWithIslands,
  uploadDocFile,
  deleteDocFile,
  createDoc,
  updateDoc,
  deleteDocRow,
} from '../api/docs';
import {
  UPLOAD_ACCEPT,
  FILE_CATEGORY_LABELS,
  categorizeFileType,
} from '@gmnl/shared';
import type { AdminDoc, Institution, DocUpsertRequest } from '@gmnl/shared';

const { Title, Text } = Typography;

export default function DocsManagePage() {
  const [docs, setDocs] = useState<AdminDoc[]>([]);
  const [institutions, setInstitutions] = useState<Institution[]>([]);
  const [loading, setLoading] = useState(false);
  const [instFilter, setInstFilter] = useState<number | null>(null);
  const [islandFilter, setIslandFilter] = useState<number | null>(null);
  const [keyword, setKeyword] = useState('');

  // 文件上传 modal
  const [uploadTarget, setUploadTarget] = useState<AdminDoc | null>(null);
  const [uploading, setUploading] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  // 文档行 新增/编辑 modal
  const [docModalOpen, setDocModalOpen] = useState(false);
  const [editingDoc, setEditingDoc] = useState<AdminDoc | null>(null);
  const [docSaving, setDocSaving] = useState(false);
  const [docForm] = Form.useForm<DocUpsertRequest>();
  const watchedInstId = Form.useWatch('institutionId', docForm);

  const load = async () => {
    setLoading(true);
    try {
      const [d, i] = await Promise.all([fetchAdminDocs(), fetchInstitutionsWithIslands()]);
      setDocs(d);
      setInstitutions(i);
    } catch {
      message.error('加载失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const instName = useMemo(() => {
    const m = new Map<number, string>();
    institutions.forEach((i) => m.set(i.id, i.name));
    return m;
  }, [institutions]);

  const islandName = useMemo(() => {
    const m = new Map<number, string>();
    institutions.forEach((i) => i.islands.forEach((is) => m.set(is.id, is.name)));
    return m;
  }, [institutions]);

  const islandsOfInst = (instId?: number) =>
    institutions.find((i) => i.id === instId)?.islands ?? [];

  const filtered = useMemo(() => {
    const kw = keyword.trim().toLowerCase();
    return docs.filter((d) =>
      (!instFilter || d.institutionId === instFilter) &&
      (!islandFilter || d.islandId === islandFilter) &&
      (!kw || d.title.toLowerCase().includes(kw)),
    );
  }, [docs, instFilter, islandFilter, keyword]);

  // ---- 文件上传 ----
  const openUpload = (doc: AdminDoc) => {
    setSelectedFile(null);
    setUploadTarget(doc);
  };

  const doUpload = async () => {
    if (!uploadTarget || !selectedFile) return;
    setUploading(true);
    try {
      await uploadDocFile(uploadTarget.id, selectedFile);
      message.success('上传成功');
      setUploadTarget(null);
      setSelectedFile(null);
      await load();
    } catch (e: any) {
      message.error(e?.response?.data?.message || '上传失败');
    } finally {
      setUploading(false);
    }
  };

  const doDeleteFile = async (doc: AdminDoc) => {
    try {
      await deleteDocFile(doc.id);
      message.success('已删除文件');
      await load();
    } catch (e: any) {
      message.error(e?.response?.data?.message || '删除失败');
    }
  };

  // ---- 文档行 CRUD ----
  const openCreateDoc = () => {
    setEditingDoc(null);
    docForm.resetFields();
    docForm.setFieldsValue({ required: false, active: true, order: 0, institutionId: instFilter ?? undefined });
    setDocModalOpen(true);
  };

  const openEditDoc = (doc: AdminDoc) => {
    setEditingDoc(doc);
    docForm.setFieldsValue({
      title: doc.title,
      category: doc.category ?? undefined,
      institutionId: doc.institutionId,
      islandId: doc.islandId,
      required: doc.required,
      order: doc.order,
      active: doc.active,
    });
    setDocModalOpen(true);
  };

  const submitDoc = async (v: DocUpsertRequest) => {
    setDocSaving(true);
    try {
      const req: DocUpsertRequest = {
        ...v,
        category: v.category ?? null,
        order: v.order ?? null,
      };
      if (editingDoc) await updateDoc(editingDoc.id, req);
      else await createDoc(req);
      message.success(editingDoc ? '已更新' : '已创建');
      setDocModalOpen(false);
      await load();
    } catch (e: any) {
      message.error(e?.response?.data?.message || '操作失败');
    } finally {
      setDocSaving(false);
    }
  };

  const doDeleteRow = async (doc: AdminDoc) => {
    try {
      await deleteDocRow(doc.id);
      message.success('已删除');
      await load();
    } catch (e: any) {
      message.error(e?.response?.data?.message || '删除失败');
    }
  };

  const columns: ColumnsType<AdminDoc> = [
    {
      title: '文档标题', dataIndex: 'title', key: 'title',
      render: (t, r) => <Space size={4}>{!r.active && <Tag>已停用</Tag>}{t}</Space>,
    },
    { title: '机构', key: 'inst', width: 90, render: (_, r) => instName.get(r.institutionId) ?? r.institutionId },
    { title: '小岛', key: 'island', width: 110, render: (_, r) => islandName.get(r.islandId) ?? r.islandId },
    {
      title: '必修', key: 'required', width: 70,
      render: (_, r) => r.required ? <Tag color="green">必修</Tag> : <Tag>选修</Tag>,
    },
    {
      title: '文件', key: 'file', width: 150,
      render: (_, r) => r.fileType ? (
        <Space direction="vertical" size={0}>
          <Text>{r.fileType.toUpperCase()}</Text>
          <Text type="secondary">{FILE_CATEGORY_LABELS[categorizeFileType(r.fileType)]}</Text>
        </Space>
      ) : <Tag>未上传</Tag>,
    },
    {
      title: '上传时间', key: 'uploadedAt', width: 160,
      render: (_, r) => r.uploadedAt ? new Date(r.uploadedAt).toLocaleString('zh-CN') : '—',
    },
    {
      title: '操作', key: 'actions', width: 260, fixed: 'right',
      render: (_, r) => (
        <Space size={4} wrap>
          <Button size="small" onClick={() => openEditDoc(r)}>编辑</Button>
          <Button size="small" onClick={() => openUpload(r)}>
            {r.fileType ? '替换文件' : '上传文件'}
          </Button>
          <Button size="small" danger disabled={!r.fileType} onClick={() => doDeleteFile(r)}>删除文件</Button>
          <Popconfirm
            title="删除该文档？"
            description="有学习记录时将拒绝，请改用停用。"
            onConfirm={() => doDeleteRow(r)}
          >
            <Button size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <Card
      title={<Title level={4} style={{ margin: 0 }}>文档管理</Title>}
      extra={<Space><Button type="primary" onClick={openCreateDoc}>新增文档</Button><Button onClick={load} loading={loading}>刷新</Button></Space>}
    >
      <Space wrap style={{ marginBottom: 16 }}>
        <Select
          style={{ width: 160 }}
          placeholder="按机构筛选"
          allowClear
          value={instFilter ?? undefined}
          onChange={(v) => { setInstFilter(v ?? null); setIslandFilter(null); }}
          options={institutions.map((i) => ({ value: i.id, label: i.name }))}
        />
        <Select
          style={{ width: 160 }}
          placeholder="按小岛筛选"
          allowClear
          value={islandFilter ?? undefined}
          onChange={(v) => setIslandFilter(v ?? null)}
          disabled={!instFilter}
          options={islandsOfInst(instFilter ?? undefined).map((is) => ({ value: is.id, label: is.name }))}
        />
        <Input.Search
          style={{ width: 220 }}
          placeholder="按标题搜索"
          allowClear
          onSearch={setKeyword}
          onChange={(e) => { if (!e.target.value) setKeyword(''); }}
        />
      </Space>

      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={filtered}
        pagination={{ pageSize: 20, showSizeChanger: false }}
        scroll={{ x: 1000 }}
      />

      {/* 文件上传 modal */}
      <Modal
        open={!!uploadTarget}
        title={uploadTarget?.fileType ? '替换文件' : '上传文件'}
        onCancel={() => !uploading && setUploadTarget(null)}
        footer={
          <Space>
            <Button onClick={() => setUploadTarget(null)} disabled={uploading}>取消</Button>
            <Button type="primary" onClick={doUpload} loading={uploading} disabled={!selectedFile}>上传</Button>
          </Space>
        }
      >
        {uploadTarget && (
          <div>
            <p style={{ marginBottom: 8 }}>文档：<Text strong>{uploadTarget.title}</Text></p>
            <p style={{ color: '#888', marginBottom: 12 }}>
              支持：{FILE_CATEGORY_LABELS.DOCUMENT} / {FILE_CATEGORY_LABELS.IMAGE} / {FILE_CATEGORY_LABELS.VIDEO} / {FILE_CATEGORY_LABELS.AUDIO} / {FILE_CATEGORY_LABELS.ARCHIVE}（上限 500MB）
            </p>
            <input
              type="file"
              accept={UPLOAD_ACCEPT}
              onChange={(e) => setSelectedFile(e.target.files?.[0] ?? null)}
              style={{ width: '100%' }}
            />
            {selectedFile && (
              <p style={{ marginTop: 8, color: '#888' }}>
                已选：{selectedFile.name}（{(selectedFile.size / 1024 / 1024).toFixed(2)} MB）
              </p>
            )}
          </div>
        )}
      </Modal>

      {/* 文档行 新增/编辑 modal */}
      <Modal
        open={docModalOpen}
        title={editingDoc ? '编辑文档' : '新增文档'}
        onCancel={() => setDocModalOpen(false)}
        confirmLoading={docSaving}
        onOk={() => docForm.submit()}
        width={560}
      >
        <Form form={docForm} layout="vertical" onFinish={submitDoc}>
          <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入标题' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="category" label="分类">
            <Input placeholder="如 关于公司" />
          </Form.Item>
          <Space wrap>
            <Form.Item name="institutionId" label="机构" rules={[{ required: true, message: '请选择机构' }]} style={{ width: 200 }}>
              <Select
                options={institutions.map((i) => ({ value: i.id, label: i.name }))}
                placeholder="选择机构"
                onChange={() => docForm.setFieldValue('islandId', undefined)}
              />
            </Form.Item>
            <Form.Item name="islandId" label="小岛" rules={[{ required: true, message: '请选择小岛' }]} style={{ width: 200 }}>
              <Select
                options={islandsOfInst(watchedInstId).map((is) => ({ value: is.id, label: is.name }))}
                placeholder="选择小岛"
                disabled={!watchedInstId}
              />
            </Form.Item>
          </Space>
          <Space wrap>
            <Form.Item name="order" label="排序" initialValue={0}>
              <InputNumber min={0} />
            </Form.Item>
            <Form.Item name="required" label="必修" valuePropName="checked" initialValue={false}>
              <Switch />
            </Form.Item>
            <Form.Item name="active" label="启用" valuePropName="checked" initialValue={true}>
              <Switch />
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </Card>
  );
}
