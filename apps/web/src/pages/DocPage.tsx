import { useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { docStreamUrl } from '../api/content';
import { useProgressStore } from '../stores/progressStore';
import client from '../api/client';
import { categorizeFileType } from '@gmnl/shared';
import type { Doc, DocStatus } from '@gmnl/shared';

export default function DocPage() {
  const { docId } = useParams();
  const [doc, setDoc] = useState<Doc | null>(null);
  const [error, setError] = useState<string | null>(null);
  const upsertProgress = useProgressStore((s) => s.upsertProgress);
  const completeDoc = useProgressStore((s) => s.completeDoc);
  const aggregate = useProgressStore((s) => s.aggregate);
  const pctRef = useRef(0);
  const lastReportRef = useRef(0);

  // 进入即 READING
  useEffect(() => {
    if (!docId) return;
    fetchDoc(Number(docId)).then(setDoc).catch(() => setError('加载失败，请重试'));
    upsertProgress(Number(docId), 'READING', 1);
  }, [docId, upsertProgress]);

  // 滚动驱动进度
  useEffect(() => {
    const onScroll = () => {
      const el = document.documentElement;
      const total = el.scrollHeight - el.clientHeight;
      const pct = total > 0 ? Math.min(100, Math.round((el.scrollTop / total) * 100)) : 100;
      pctRef.current = Math.max(pctRef.current, pct);
      const now = Date.now();
      if (pct - lastReportRef.current >= 5 && now - lastReportTime() >= 10000) {
        lastReportRef.current = pct;
        lastReportTs = now;
        upsertProgress(Number(docId), 'READING', pct);
      }
    };
    window.addEventListener('scroll', onScroll);
    return () => window.removeEventListener('scroll', onScroll);
  }, [docId, upsertProgress]);

  // 离开强制上报最终值
  useEffect(() => {
    return () => {
      if (docId && pctRef.current > 0) {
        upsertProgress(Number(docId), 'READING', pctRef.current);
      }
    };
  }, [docId, upsertProgress]);

  const currentPct = aggregate?.documents.find((p) => p.docId === Number(docId))?.progressPct ?? 0;

  // 媒体走流式直链(后端 Range 206,<video> 等原生 seek/分片,不占内存);下载另走 blob。
  const download = async () => {
    if (!doc) return;
    const res = await client.get(`/docs/${doc.id}/file`, { responseType: 'blob' });
    const url = URL.createObjectURL(res.data);
    const a = document.createElement('a');
    a.href = url;
    a.download = doc.title;
    a.click();
    URL.revokeObjectURL(url);
  };

  const renderPreview = () => {
    if (!doc?.fileType) {
      return <div style={{ color: 'var(--slate-400)' }}>该文档尚未上传文件（占位）</div>;
    }
    const cat = categorizeFileType(doc.fileType);
    const url = docStreamUrl(doc.id);
    if (cat === 'IMAGE') {
      return <img src={url} alt={doc.title} style={{ maxWidth: '100%', borderRadius: 8 }} />;
    }
    if (cat === 'VIDEO') {
      return <video src={url} controls style={{ maxWidth: '100%', borderRadius: 8 }} />;
    }
    if (cat === 'AUDIO') {
      return <audio src={url} controls style={{ width: '100%' }} />;
    }
    if (cat === 'DOCUMENT' && doc.fileType === 'pdf') {
      return <iframe src={url} title={doc.title} style={{ width: '100%', height: '70vh', border: '1px solid var(--slate-200)', borderRadius: 8 }} />;
    }
    return <div style={{ color: 'var(--slate-400)' }}>该类型不支持在线预览，请下载查看。</div>;
  };

  return (
    <div className="page-shell">
      {error && <div style={{ color: 'red', padding: 16 }}>{error}</div>}
      <Link to={doc ? `/island/${doc.islandId}` : '/'} className="page-link-back">← 返回</Link>
      {doc && (
        <>
          <h1 className="page-title">{doc.title}</h1>
          <div className="doc-progress" style={{ height: 8 }}>
            <div style={{ width: `${currentPct}%` }} />
          </div>
          <div style={{ color: 'var(--slate-500)', fontSize: 13, margin: '8px 0 16px' }}>当前进度 {currentPct}%</div>
          {doc.fileType && (
            <div style={{ marginBottom: 16 }}>
              <button className="btn-secondary" onClick={download}>下载文件（{doc.fileType.toUpperCase()}）</button>
            </div>
          )}
          <div style={{ marginTop: 8 }}>{renderPreview()}</div>
          <div style={{ marginTop: 24 }}>
            <button className="btn-primary" onClick={() => completeDoc(Number(docId))}>标记完成</button>
          </div>
        </>
      )}
    </div>
  );
}

let lastReportTs = 0;
function lastReportTime() { return lastReportTs; }
export function _setLastReportTime(ts: number) { lastReportTs = ts; } // 测试钩子

async function fetchDoc(id: number): Promise<Doc | null> {
  const { data } = await client.get(`/docs/${id}`);
  return data;
}
