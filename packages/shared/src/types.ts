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

export type DocFileType = 'pdf' | 'docx' | 'pptx' | 'xlsx';

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
