import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchInstitutions } from '../api/content';
import { useProgressStore } from '../stores/progressStore';
import { useAuthStore } from '../stores/authStore';
import { INSTITUTION_GEO, INSTITUTION_GEO_FALLBACK } from '@gmnl/shared';
import type { Institution, IslandStateView } from '@gmnl/shared';

/** 中国地图首页:机构作为浮动卡片按地理位置铺在地图上,点击进入该机构的海洋小岛页。 */
export default function VoyagePage() {
  const navigate = useNavigate();
  const [institutions, setInstitutions] = useState<Institution[]>([]);
  const [error, setError] = useState<string | null>(null);
  const aggregate = useProgressStore((s) => s.aggregate);
  const loadProgress = useProgressStore((s) => s.loadProgress);
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);

  useEffect(() => {
    fetchInstitutions().then(setInstitutions).catch(() => setError('加载失败，请重试'));
    loadProgress();
  }, [loadProgress]);

  // 小岛状态聚合:islandId → {completed,total}
  const islandMap = new Map<number, IslandStateView>();
  aggregate?.islands.forEach((i) => islandMap.set(i.islandId, i));

  // 机构完成度:其下所有小岛完成数之和 / 小岛总数
  const instProgress = (inst: Institution): { done: number; total: number } => {
    let done = 0;
    let total = 0;
    inst.islands.forEach((isl) => {
      const st = islandMap.get(isl.id);
      total += 1;
      if (st?.status === 'COMPLETED') done += 1;
    });
    return { done, total };
  };

  return (
    <div className="map-home">
      {/* 顶栏 */}
      <nav className="voyage-top-nav">
        <img src="/brand-logo.png" alt="国民养老保险" className="voyage-nav-logo" />
        <div className="voyage-nav-right">
          <span className="voyage-nav-name">{user?.name ?? '新航员'}</span>
          <button className="voyage-nav-btn" onClick={logout}>退出</button>
        </div>
      </nav>

      {/* 标题 */}
      <div className="map-home-header">
        <h1 className="map-home-title">国民养老 · 新人航行计划</h1>
        <p className="map-home-subtitle">点击机构入口，开启你的知识海洋之旅</p>
      </div>

      {error && <div style={{ color: 'red', padding: 16, textAlign: 'center' }}>{error}</div>}

      {/* 地图 + 机构卡片 */}
      <div className="map-home-container">
        <img src="/china-map.png" alt="中国地图" className="map-home-bg" draggable={false} />
        {institutions.map((inst) => {
          const geo = INSTITUTION_GEO[inst.key] ?? INSTITUTION_GEO_FALLBACK;
          const { done, total } = instProgress(inst);
          return (
            <div
              key={inst.id}
              className="inst-banner"
              style={{ top: geo.top, left: geo.left, width: 150 }}
              title={inst.name}
              onClick={() => navigate(`/institution/${inst.id}`)}
            >
              {geo.img && <img src={geo.img} alt={inst.name} draggable={false} />}
              <div className="inst-label">{geo.emoji} {inst.name}</div>
              <div className="inst-prog">{total > 0 ? `${done}/${total} 小岛` : ''}</div>
            </div>
          );
        })}
      </div>

      <div className="map-home-hint">👆 点击机构卡片开始学习</div>
    </div>
  );
}
