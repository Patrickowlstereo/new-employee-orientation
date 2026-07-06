import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { fetchDocs } from '../api/content';
import { useProgressStore } from '../stores/progressStore';
import type { Doc } from '@gmnl/shared';

export default function IslandPage() {
  const { islandId } = useParams();
  const [docs, setDocs] = useState<Doc[]>([]);
  const [error, setError] = useState<string | null>(null);
  const aggregate = useProgressStore((s) => s.aggregate);
  const loadProgress = useProgressStore((s) => s.loadProgress);

  useEffect(() => {
    if (!islandId) return;
    fetchDocs(Number(islandId)).then(setDocs).catch(() => setError('加载失败，请重试'));
    loadProgress();
  }, [islandId, loadProgress]);

  const progMap = new Map<number, number>();
  aggregate?.documents.forEach((p) => progMap.set(p.docId, p.progressPct));

  // 返回该小岛所属机构页;无文档时回首页
  const backTo = docs[0] ? `/institution/${docs[0].institutionId}` : '/';

  return (
    <div className="page-shell">
      {error && <div style={{ color: 'red', padding: 16 }}>{error}</div>}
      <Link to={backTo} className="page-link-back">← 返回群岛</Link>
      <h1 className="page-title">小岛文档</h1>
      {docs.map((d) => {
        const pct = progMap.get(d.id) ?? 0;
        return (
          <Link key={d.id} to={`/doc/${d.id}`} className="doc-card">
            <div className="doc-card-title">
              <span className={d.required ? 'req-star' : 'opt-dot'}>{d.required ? '★' : '○'}</span>
              {d.title}
              {d.fileType && (
                <span style={{ marginLeft: 8, fontSize: 12, color: 'var(--slate-400)', fontWeight: 400 }}>
                  {d.fileType.toUpperCase()}
                </span>
              )}
            </div>
            <div className="doc-progress"><div style={{ width: `${pct}%` }} /></div>
          </Link>
        );
      })}
      {docs.length === 0 && !error && (
        <div style={{ color: 'var(--slate-400)', padding: 16 }}>该小岛暂无文档</div>
      )}
    </div>
  );
}
