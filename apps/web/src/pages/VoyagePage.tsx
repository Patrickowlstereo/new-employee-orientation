import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchInstitutions } from '../api/content';
import { useProgressStore } from '../stores/progressStore';
import { useAuthStore } from '../stores/authStore';
import type { Institution, IslandStateView } from '@gmnl/shared';

export default function VoyagePage() {
  const [institutions, setInstitutions] = useState<Institution[]>([]);
  const aggregate = useProgressStore((s) => s.aggregate);
  const loadProgress = useProgressStore((s) => s.loadProgress);
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);

  useEffect(() => {
    fetchInstitutions().then(setInstitutions);
    loadProgress();
  }, [loadProgress]);

  const islandMap = new Map<number, IslandStateView>();
  aggregate?.islands.forEach((i) => islandMap.set(i.islandId, i));

  return (
    <div style={{ fontFamily: 'sans-serif', padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 24 }}>
        <h1>新人航行计划</h1>
        <div>
          <span style={{ marginRight: 12 }}>{user?.name}</span>
          <button onClick={logout}>退出</button>
        </div>
      </div>
      {institutions.map((inst) => (
        <div key={inst.id} style={{ marginBottom: 24, padding: 16, border: '1px solid #ddd', borderRadius: 8 }}>
          <h2>{inst.name}</h2>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12 }}>
            {inst.islands.map((isl) => {
              const st = islandMap.get(isl.id);
              const label = st?.status === 'COMPLETED' ? '✅' : st?.status === 'UNLOCKED' ? '🔓' : '🔒';
              return (
                <Link key={isl.id} to={`/island/${isl.id}`} style={{ textDecoration: 'none', color: '#333' }}>
                  <div style={{ padding: 12, background: '#f5f7fb', borderRadius: 8, minWidth: 120 }}>
                    <div>{label} {isl.name}</div>
                    <div style={{ fontSize: 12, color: '#888' }}>
                      {st ? `${st.completedCount}/${st.totalCount}` : '0/0'}
                    </div>
                  </div>
                </Link>
              );
            })}
          </div>
        </div>
      ))}
    </div>
  );
}
