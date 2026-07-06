import { create } from 'zustand';
import type { User, LoginRequest } from '@gmnl/shared';
import client from '../api/client';

interface AuthState {
  user: User | null;
  token: string | null;
  login: (req: LoginRequest) => Promise<void>;
  fetchMe: () => Promise<boolean>;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  token: localStorage.getItem('admin_token'),
  login: async (req) => {
    const { data } = await client.post('/auth/login', req);
    if (data.user.role !== 'ADMIN') throw new Error('非管理员账号');
    localStorage.setItem('admin_token', data.token);
    set({ token: data.token, user: data.user });
  },
  fetchMe: async () => {
    try {
      const { data } = await client.get('/auth/me');
      if (data.role !== 'ADMIN') { localStorage.removeItem('admin_token'); set({ token: null, user: null }); return false; }
      set({ user: data }); return true;
    } catch { localStorage.removeItem('admin_token'); set({ token: null, user: null }); return false; }
  },
  logout: () => { localStorage.removeItem('admin_token'); set({ token: null, user: null }); },
}));
