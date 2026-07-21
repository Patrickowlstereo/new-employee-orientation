import { create } from 'zustand';
import type { ProgressAggregate, DocStatus, UpsertProgressRequest } from '@gmnl/shared';
import client from '../api/client';

interface ProgressState {
  aggregate: ProgressAggregate | null;
  loadProgress: () => Promise<void>;
  /** reload=false 用于卸载/后台上报：只写不拉，避免无效请求。 */
  upsertProgress: (docId: number, status: DocStatus, progressPct: number, reload?: boolean) => Promise<void>;
  /** 返回是否成功，供按钮给用户明确反馈。 */
  completeDoc: (docId: number) => Promise<boolean>;
}

export const useProgressStore = create<ProgressState>((set, get) => ({
  aggregate: null,
  loadProgress: async () => {
    const { data } = await client.get('/progress');
    set({ aggregate: data });
  },
  upsertProgress: async (docId, status, progressPct, reload = true) => {
    // 进度上报属尽力而为：失败静默降级，不产生 unhandled rejection
    try {
      const body: UpsertProgressRequest = { status, progressPct };
      await client.put(`/progress/${docId}`, body);
      // 简化：上报后重新拉取聚合（后端单调不回退保证正确）
      if (reload) await get().loadProgress();
    } catch {
      // 静默：下次节流上报或列表页重新加载会自愈
    }
  },
  completeDoc: async (docId) => {
    try {
      await client.post(`/progress/${docId}/complete`);
      await get().loadProgress();
      return true;
    } catch {
      return false;
    }
  },
}));
