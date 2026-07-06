import { useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { fetchDocs } from '../api/content';
import { docFileUrl } from '../api/content';
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

  // 按文件类型取 blob 预览（图片/视频/音频/PDF）；office/压缩等仅下载。
  // 注：整文件入内存，大视频流式预览留待阶段 6。
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [previewError, setPreviewError] = useState(false);
  useEffect(() => {
    if (!doc?.fileType) { setPreviewUrl(null); setPreviewError(false); return; }
    const cat = categorizeFileType(doc.fileType);
    if (cat !== 'IMAGE' && cat !== 'VIDEO' && cat !== 'AUDIO' && cat !== 'DOCUMENT') {
      setPreviewUrl(null); return;
    }
    if (cat === 'DOCUMENT' && doc.fileType !== 'pdf') { setPreviewUrl(null); return; }
    let url: string | null = null;
    let cancelled = false;
    client.get(`/docs/${doc.id}/file`, { responseType: 'blob' })
      .then((res) => { if (cancelled) return; url = URL.createObjectURL(res.data); setPreviewUrl(url); })
      .catch(() => { if (!cancelled) setPreviewError(true); });
    return () => { cancelled = true; if (url) URL.revokeObjectURL(url); };
  }, [doc]);

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
      return <div style={{ color: '#999' }}>该文档尚未上传文件（占位）</div>;
    }
    const cat = categorizeFileType(doc.fileType);
    if (previewError) {
      return <div style={{ color: 'red' }}>预览加载失败，请使用下载</div>;
    }
    if (previewUrl) {
      if (cat === 'IMAGE') {
        return <img src={previewUrl} alt={doc.title} style={{ maxWidth: '100%', borderRadius: 8 }} />;
      }
      if (cat === 'VIDEO') {
        return <video src={previewUrl} controls style={{ maxWidth: '100%', borderRadius: 8 }} />;
      }
      if (cat === 'AUDIO') {
        return <audio src={previewUrl} controls style={{ width: '100%' }} />;
      }
      if (cat === 'DOCUMENT' && doc.fileType === 'pdf') {
        return <iframe src={previewUrl} title={doc.title} style={{ width: '100%', height: '70vh', border: '1px solid #eee', borderRadius: 8 }} />;
      }
    }
    // office/压缩等不支持浏览器内预览的类型，或加载中
    if (cat === 'DOCUMENT' || cat === 'ARCHIVE' || cat === 'OTHER') {
      return <div style={{ color: '#999' }}>该类型不支持在线预览，请下载查看。</div>;
    }
    return <div style={{ color: '#999' }}>预览加载中…</div>;
  };

  return (
    <div style={{ fontFamily: 'sans-serif', padding: 24 }}>
      {error && <div style={{color:'red',padding:16}}>{error}</div>}
      <Link to={doc ? `/island/${doc.islandId}` : '/'}>← 返回</Link>
      {doc && (
        <>
          <h1>{doc.title}</h1>
          <div style={{ height: 8, background: '#eee', borderRadius: 4, margin: '12px 0' }}>
            <div style={{ width: `${currentPct}%`, height: '100%', background: '#4BBF92', borderRadius: 4 }} />
          </div>
          <div style={{ color: '#888', marginBottom: 16 }}>当前进度 {currentPct}%</div>
          {doc.fileType && (
            <div style={{ marginBottom: 16 }}>
              <button onClick={download}>下载文件（{doc.fileType.toUpperCase()}）</button>
            </div>
          )}
          <div style={{ marginTop: 8 }}>{renderPreview()}</div>
          <div style={{ marginTop: 24 }}>
            <button onClick={() => completeDoc(Number(docId))}>标记完成</button>
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
