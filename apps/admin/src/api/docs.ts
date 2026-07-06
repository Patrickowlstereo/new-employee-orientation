import client from './client';
import type { AdminDoc, Institution } from '@gmnl/shared';

export const fetchAdminDocs = async (): Promise<AdminDoc[]> =>
  (await client.get('/admin/docs')).data;

export const fetchInstitutionsWithIslands = async (): Promise<Institution[]> =>
  (await client.get('/institutions')).data;

export const uploadDocFile = async (docId: number, file: File): Promise<AdminDoc> => {
  const form = new FormData();
  form.append('file', file);
  const { data } = await client.post(`/admin/docs/${docId}/upload`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 0, // 大文件上传不设超时
  });
  return data;
};

export const deleteDocFile = async (docId: number): Promise<AdminDoc> =>
  (await client.delete(`/admin/docs/${docId}/file`)).data;
