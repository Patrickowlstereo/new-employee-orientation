import client from './client';
import type { UserStats, StatsOverview } from '@gmnl/shared';

export const fetchUserStats = async (): Promise<UserStats[]> =>
  (await client.get('/admin/stats/users')).data;

export const fetchStatsOverview = async (): Promise<StatsOverview> =>
  (await client.get('/admin/stats/overview')).data;
