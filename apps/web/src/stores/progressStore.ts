import { create } from 'zustand';
import type { ProgressAggregate, DocStatus, UpsertProgressRequest } from '@gmnl/shared';
import client from '../api/client';

interface ProgressState {
  aggregate: ProgressAggregate | null;
  loadProgress: () => Promise<void>;
  upsertProgress: (docId: number, status: DocStatus, progressPct: number) => Promise<void>;
  completeDoc: (docId: number) => Promise<void>;
}

export const useProgressStore = create<ProgressState>((set, get) => ({
  aggregate: null,
  loadProgress: async () => {
    const { data } = await client.get('/progress');
    set({ aggregate: data });
  },
  upsertProgress: async (docId, status, progressPct) => {
    const body: UpsertProgressRequest = { status, progressPct };
    await client.put(`/progress/${docId}`, body);
    // 简化：上报后重新拉取聚合（后端单调不回退保证正确）
    await get().loadProgress();
  },
  completeDoc: async (docId) => {
    await client.post(`/progress/${docId}/complete`);
    await get().loadProgress();
  },
}));
