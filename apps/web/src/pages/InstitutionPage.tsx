import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { fetchInstitutions } from '../api/content';
import { useProgressStore } from '../stores/progressStore';
import type { Institution, IslandStateView, IslandStatus } from '@gmnl/shared';

/** 机构海洋小岛页:该机构下的小岛沿弧线排布,岛间 SVG 航线连接,按进度状态解锁/完成。 */
export default function InstitutionPage() {
  const { institutionId } = useParams();
  const navigate = useNavigate();
  const [institution, setInstitution] = useState<Institution | null>(null);
  const [error, setError] = useState<string | null>(null);
  const aggregate = useProgressStore((s) => s.aggregate);
  const loadProgress = useProgressStore((s) => s.loadProgress);

  useEffect(() => {
    fetchInstitutions()
      .then((all) => {
        const found = all.find((i) => i.id === Number(institutionId)) ?? null;
        setInstitution(found);
        if (!found) setError('未找到该机构');
      })
      .catch(() => setError('加载失败，请重试'));
    loadProgress();
  }, [institutionId, loadProgress]);

  const islandMap = new Map<number, IslandStateView>();
  aggregate?.islands.forEach((i) => islandMap.set(i.islandId, i));

  const islands = institution?.islands ?? [];
  const n = islands.length;
  const positions = islands.map((_, i) => {
    const x = n > 1 ? ((i + 1) / (n + 1)) * 100 : 50;
    const t = n > 1 ? i / (n - 1) : 0.5;
    const y = 55 - 20 * Math.sin(t * Math.PI); // 弧线:中部小岛略高
    return { x, y };
  });

  const statusOf = (islandId: number): IslandStatus =>
    islandMap.get(islandId)?.status ?? 'LOCKED';
  const countsOf = (islandId: number) => {
    const st = islandMap.get(islandId);
    return { done: st?.completedCount ?? 0, total: st?.totalCount ?? 0 };
  };

  const badgeText = (islandId: number) => {
    const s = statusOf(islandId);
    const { done, total } = countsOf(islandId);
    if (s === 'LOCKED') return '🔒 未解锁';
    if (s === 'COMPLETED') return `✅ ${done}/${total}`;
    return `🔓 ${done}/${total}`;
  };

  return (
    <div className="ocean-page">
      <Link to="/" className="ocean-back">← 返回</Link>

      <div className="ocean-header">
        <div className="ocean-title">{institution?.name ?? '机构'} · 知识群岛</div>
      </div>

      {error && <div style={{ color: '#fff', textAlign: 'center', padding: 24 }}>{error}</div>}

      {/* 精炼星光 */}
      <div className="ocean-sparkles">
        {SPARKLES.map((s, i) => (
          <span key={i} style={{ left: s.left, top: s.top, animationDelay: s.delay }} />
        ))}
      </div>

      <div className="ocean-islands">
        {n > 1 && (
          <svg viewBox="0 0 100 100" preserveAspectRatio="none">
            {positions.slice(0, -1).map((p, i) => {
              const next = positions[i + 1];
              const active = statusOf(islands[i].id) !== 'LOCKED';
              return (
                <path
                  key={i}
                  className="island-conn"
                  d={`M ${p.x} ${p.y} L ${next.x} ${next.y}`}
                  style={{
                    vectorEffect: 'non-scaling-stroke',
                    opacity: active ? 0.8 : 0.3,
                  }}
                />
              );
            })}
          </svg>
        )}

        {islands.map((isl, i) => {
          const p = positions[i];
          const st = statusOf(isl.id);
          const clickable = st !== 'LOCKED';
          return (
            <div
              key={isl.id}
              className={`island-node is-${st.toLowerCase()}`}
              style={{ top: `${p.y}%`, left: `${p.x}%`, animationDelay: `${i * 0.4}s` }}
              onClick={() => clickable && navigate(`/island/${isl.id}`)}
            >
              <div className="island-glow">
                <div className="island-icon">🏝️</div>
                <div className="island-name">{isl.name}</div>
              </div>
              <div className="island-badge">{badgeText(isl.id)}</div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

// 星光位置(固定,避免渲染期随机)
const SPARKLES = [
  { left: '12%', top: '18%', delay: '0s' },
  { left: '30%', top: '52%', delay: '0.6s' },
  { left: '48%', top: '24%', delay: '1.1s' },
  { left: '66%', top: '46%', delay: '1.7s' },
  { left: '82%', top: '20%', delay: '2.2s' },
  { left: '24%', top: '72%', delay: '0.9s' },
  { left: '56%', top: '68%', delay: '1.6s' },
  { left: '88%', top: '60%', delay: '2.5s' },
];
