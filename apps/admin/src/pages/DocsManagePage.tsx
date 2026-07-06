import { useEffect, useMemo, useRef, useState } from 'react';
import { Card, Table, Select, Button, Space, Tag, Modal, message, Popconfirm, Typography, Input } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  fetchAdminDocs,
  fetchInstitutionsWithIslands,
  uploadDocFile,
  deleteDocFile,
} from '../api/docs';
import {
  UPLOAD_ACCEPT,
  FILE_CATEGORY_LABELS,
  categorizeFileType,
} from '@gmnl/shared';
import type { AdminDoc, Institution } from '@gmnl/shared';

const { Title, Text } = Typography;

export default function DocsManagePage() {
  const [docs, setDocs] = useState<AdminDoc[]>([]);
  const [institutions, setInstitutions] = useState<Institution[]>([]);
  const [loading, setLoading] = useState(false);
  const [instFilter, setInstFilter] = useState<number | null>(null);
  const [islandFilter, setIslandFilter] = useState<number | null>(null);
  const [keyword, setKeyword] = useState('');

  const [uploadTarget, setUploadTarget] = useState<AdminDoc | null>(null);
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

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

  const islandsOfInst = useMemo(
    () => institutions.find((i) => i.id === instFilter)?.islands ?? [],
    [institutions, instFilter],
  );

  const filtered = useMemo(() => {
    const kw = keyword.trim().toLowerCase();
    return docs.filter((d) =>
      (!instFilter || d.institutionId === instFilter) &&
      (!islandFilter || d.islandId === islandFilter) &&
      (!kw || d.title.toLowerCase().includes(kw)),
    );
  }, [docs, instFilter, islandFilter, keyword]);

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

  const doDelete = async (doc: AdminDoc) => {
    try {
      await deleteDocFile(doc.id);
      message.success('已删除文件');
      await load();
    } catch (e: any) {
      message.error(e?.response?.data?.message || '删除失败');
    }
  };

  const columns: ColumnsType<AdminDoc> = [
    { title: '文档标题', dataIndex: 'title', key: 'title' },
    { title: '机构', key: 'inst', width: 90, render: (_, r) => instName.get(r.institutionId) ?? r.institutionId },
    { title: '小岛', key: 'island', width: 110, render: (_, r) => islandName.get(r.islandId) ?? r.islandId },
    {
      title: '必修', key: 'required', width: 70,
      render: (_, r) => r.required ? <Tag color="green">必修</Tag> : <Tag>选修</Tag>,
    },
    {
      title: '文件', key: 'file', width: 160,
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
      title: '操作', key: 'actions', width: 180, fixed: 'right',
      render: (_, r) => (
        <Space>
          <Button size="small" onClick={() => openUpload(r)}>
            {r.fileType ? '替换文件' : '上传文件'}
          </Button>
          <Popconfirm
            title="确认删除该文档的文件？"
            description="文档记录保留，仅清除已上传的文件。"
            onConfirm={() => doDelete(r)}
            disabled={!r.fileType}
          >
            <Button size="small" danger disabled={!r.fileType}>删除文件</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <Card
      title={<Title level={4} style={{ margin: 0 }}>文档管理</Title>}
      extra={<Button onClick={load} loading={loading}>刷新</Button>}
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
          options={islandsOfInst.map((is) => ({ value: is.id, label: is.name }))}
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
        scroll={{ x: 900 }}
      />

      <Modal
        open={!!uploadTarget}
        title={uploadTarget?.fileType ? '替换文件' : '上传文件'}
        onCancel={() => !uploading && setUploadTarget(null)}
        footer={
          <Space>
            <Button onClick={() => setUploadTarget(null)} disabled={uploading}>取消</Button>
            <Button type="primary" onClick={doUpload} loading={uploading} disabled={!selectedFile}>
              上传
            </Button>
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
              ref={fileInputRef}
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
    </Card>
  );
}
