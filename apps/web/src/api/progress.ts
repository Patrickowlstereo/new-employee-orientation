import client from './client';
import type { ProgressAggregate } from '@gmnl/shared';

export const fetchProgress = async (): Promise<ProgressAggregate> =>
  (await client.get('/progress')).data;
