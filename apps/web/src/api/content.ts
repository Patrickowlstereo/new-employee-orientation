import client from './client';
import type { Institution, Doc } from '@gmnl/shared';

export const fetchInstitutions = async (): Promise<Institution[]> =>
  (await client.get('/institutions')).data;

export const fetchDocs = async (islandId: number): Promise<Doc[]> =>
  (await client.get('/docs', { params: { islandId } })).data;

export const docFileUrl = (docId: number): string =>
  `/api/docs/${docId}/file`;
