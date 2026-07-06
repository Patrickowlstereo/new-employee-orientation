import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { fetchDocs } from '../api/content';
import { useProgressStore } from '../stores/progressStore';
import type { Doc } from '@gmnl/shared';

export default function IslandPage() {
  const { islandId } = useParams();
  const [docs, setDocs] = useState<Doc[]>([]);
  const aggregate = useProgressStore((s) => s.aggregate);
  const loadProgress = useProgressStore((s) => s.loadProgress);

  useEffect(() => {
    if (!islandId) return;
    fetchDocs(Number(islandId)).then(setDocs);
    loadProgress();
  }, [islandId, loadProgress]);

  const progMap = new Map<number, number>();
  aggregate?.documents.forEach((p) => progMap.set(p.docId, p.progressPct));

  return (
    <div style={{ fontFamily: 'sans-serif', padding: 24 }}>
      <Link to="/">← 返回</Link>
      <h1>小岛文档</h1>
      {docs.map((d) => (
        <Link key={d.id} to={`/doc/${d.id}`} style={{ textDecoration: 'none', color: '#333' }}>
          <div style={{ padding: 12, marginBottom: 8, border: '1px solid #eee', borderRadius: 8 }}>
            <div>{d.required ? '★' : '○'} {d.title}</div>
            <div style={{ height: 6, background: '#eee', borderRadius: 3, marginTop: 8 }}>
              <div style={{ width: `${progMap.get(d.id) ?? 0}%`, height: '100%', background: '#4A7BE0', borderRadius: 3 }} />
            </div>
          </div>
        </Link>
      ))}
    </div>
  );
}
