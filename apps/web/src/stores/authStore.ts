import { create } from 'zustand';
import type { User, LoginRequest } from '@gmnl/shared';
import client from '../api/client';

interface AuthState {
  user: User | null;
  token: string | null;
  loading: boolean;
  login: (req: LoginRequest) => Promise<void>;
  fetchMe: () => Promise<void>;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  token: localStorage.getItem('token'),
  loading: false,
  login: async (req) => {
    const { data } = await client.post('/auth/login', req);
    localStorage.setItem('token', data.token);
    set({ token: data.token, user: data.user });
  },
  fetchMe: async () => {
    set({ loading: true });
    try {
      const { data } = await client.get('/auth/me');
      set({ user: data, loading: false });
    } catch {
      localStorage.removeItem('token');
      set({ token: null, user: null, loading: false });
    }
  },
  logout: () => {
    localStorage.removeItem('token');
    set({ token: null, user: null });
  },
}));
