import client from './client';
import type { Institution, Doc } from '@gmnl/shared';

export const fetchInstitutions = async (): Promise<Institution[]> =>
  (await client.get('/institutions')).data;

export const fetchDocs = async (islandId: number): Promise<Doc[]> =>
  (await client.get('/docs', { params: { islandId } })).data;

export const docFileUrl = (docId: number): string =>
  `/api/docs/${docId}/file`;

/**
 * 媒体流式预览直链:附加 ?token= 让 <video>/<img>/<iframe> 直连文件接口。
 * 后端对该路径支持 Range(206),大视频可拖动进度条、分片加载,不占内存。
 * token 取自 localStorage;RequireAuth 已保证进入页面前存在。
 */
export const docStreamUrl = (docId: number): string => {
  const token = localStorage.getItem('token') ?? '';
  return `/api/docs/${docId}/file?token=${encodeURIComponent(token)}`;
};
