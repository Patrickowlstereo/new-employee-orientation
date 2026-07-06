import client from './client';
import type {
  Institution,
  InstitutionUpsertRequest,
  IslandUpsertRequest,
} from '@gmnl/shared';

// 机构（含小岛）只读列表，复用自 docs 模块
export { fetchInstitutionsWithIslands } from './docs';

// 机构 CRUD
export const createInstitution = async (req: InstitutionUpsertRequest): Promise<Institution> =>
  (await client.post('/admin/institutions', req)).data;

export const updateInstitution = async (id: number, req: InstitutionUpsertRequest): Promise<Institution> =>
  (await client.put(`/admin/institutions/${id}`, req)).data;

export const deleteInstitution = async (id: number): Promise<void> => {
  await client.delete(`/admin/institutions/${id}`);
};

// 小岛 CRUD
export const createIsland = async (req: IslandUpsertRequest) =>
  (await client.post('/admin/islands', req)).data;

export const updateIsland = async (id: number, req: IslandUpsertRequest) =>
  (await client.put(`/admin/islands/${id}`, req)).data;

export const deleteIsland = async (id: number): Promise<void> => {
  await client.delete(`/admin/islands/${id}`);
};
