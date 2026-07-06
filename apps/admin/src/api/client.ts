import axios from 'axios';
import { API_BASE } from '@gmnl/shared';

const client = axios.create({ baseURL: API_BASE, timeout: 15000 });

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

client.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401 && !window.location.pathname.startsWith('/admin/login')) {
      window.location.href = '/admin/login';
    }
    return Promise.reject(err);
  },
);

export default client;
