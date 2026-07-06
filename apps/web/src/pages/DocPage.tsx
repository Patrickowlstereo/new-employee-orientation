import { useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { fetchDocs } from '../api/content';
import { docFileUrl } from '../api/content';
import { useProgressStore } from '../stores/progressStore';
import client from '../api/client';
import type { Doc, DocStatus } from '@gmnl/shared';

export default function DocPage() {
  const { docId } = useParams();
  const [doc, setDoc] = useState<Doc | null>(null);
  const upsertProgress = useProgressStore((s) => s.upsertProgress);
  const completeDoc = useProgressStore((s) => s.completeDoc);
  const aggregate = useProgressStore((s) => s.aggregate);
  const pctRef = useRef(0);
  const lastReportRef = useRef(0);

  // 进入即 READING
  useEffect(() => {
    if (!docId) return;
    fetchDoc(Number(docId)).then(setDoc);
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

  return (
    <div style={{ fontFamily: 'sans-serif', padding: 24 }}>
      <Link to={doc ? `/island/${doc.islandId}` : '/'}>← 返回</Link>
      {doc && (
        <>
          <h1>{doc.title}</h1>
          <div style={{ height: 8, background: '#eee', borderRadius: 4, margin: '12px 0' }}>
            <div style={{ width: `${currentPct}%`, height: '100%', background: '#4BBF92', borderRadius: 4 }} />
          </div>
          <div style={{ color: '#888', marginBottom: 16 }}>当前进度 {currentPct}%</div>
          {doc.fileType ? (
            <button onClick={download}>下载文件</button>
          ) : (
            <div style={{ color: '#999' }}>该文档尚未上传文件（占位）</div>
          )}
          <div style={{ marginTop: 24 }}>
            <button onClick={() => completeDoc(Number(docId))}>标记完成</button>
          </div>
          <div style={{ marginTop: 32, padding: 16, background: '#f7f7f7', borderRadius: 8 }}>
            （此处为文档正文占位。阶段 4 接入真实文件预览/下载，阶段 6 迁移 Demo 视觉。）
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
