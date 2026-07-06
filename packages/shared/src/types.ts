export type UserRole = 'USER' | 'ADMIN';

export interface User {
  id: number;
  username: string;
  name: string;
  role: UserRole;
}

export interface Institution {
  id: number;
  key: string;
  name: string;
  order: number;
  islands: Island[];
}

export interface Island {
  id: number;
  key: string;
  name: string;
  order: number;
  institutionId: number;
}

// 文件扩展名（小写、无点）。阶段 4 起支持文档/图片/视频/音频/压缩等全面类型，故放宽为 string。
export type DocFileType = string;

// 文件大类，与后端 FileTypeSupport.Category 对应
export type FileCategory = 'DOCUMENT' | 'IMAGE' | 'VIDEO' | 'AUDIO' | 'ARCHIVE' | 'OTHER';

export interface Doc {
  id: number;
  title: string;
  category: string;
  institutionId: number;
  islandId: number;
  required: boolean;
  fileType: DocFileType | null;
  order: number;
  active: boolean;
}

export type DocStatus = 'NOT_STARTED' | 'READING' | 'COMPLETED';
export type IslandStatus = 'LOCKED' | 'UNLOCKED' | 'COMPLETED';

export interface ProgressItem {
  docId: number;
  status: DocStatus;
  progressPct: number;
  lastReadAt: string | null;
  completedAt: string | null;
}

export interface IslandStateView {
  islandId: number;
  status: IslandStatus;
  completedCount: number;
  totalCount: number;
}

export interface ProgressAggregate {
  documents: ProgressItem[];
  islands: IslandStateView[];
}

// 请求/响应 DTO
export interface LoginRequest {
  username: string;
  password: string;
}
export interface LoginResponse {
  token: string;
  user: User;
}
export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
}
export interface UpsertProgressRequest {
  status: DocStatus;
  progressPct: number;
}

// 后台管理用：含文件大类与上传审计的文档视图
export interface AdminDoc {
  id: number;
  title: string;
  category: string | null;
  institutionId: number;
  islandId: number;
  required: boolean;
  fileType: DocFileType | null;
  fileCategory: FileCategory;
  order: number;
  active: boolean;
  uploadedAt: string | null;
  uploadedByName: string | null;
}
