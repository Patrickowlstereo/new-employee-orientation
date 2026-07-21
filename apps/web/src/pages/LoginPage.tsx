import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useAuthStore } from '../stores/authStore';

export default function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const login = useAuthStore((s) => s.login);
  const navigate = useNavigate();

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      await login({ username: username.trim(), password });
      navigate('/', { replace: true });
    } catch (err) {
      // 401 = 凭证错误(后端明确拒绝);429 = 防爆破锁定,透传后端提示;其余视为服务不可用
      if (axios.isAxiosError(err) && err.response?.status === 401) {
        setError('账号或密码错误');
      } else if (axios.isAxiosError(err) && err.response?.status === 429) {
        setError((err.response.data as { message?: string })?.message ?? '尝试次数过多，请稍后再试');
      } else {
        setError('服务暂时不可用，请稍后再试');
      }
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="login-wrap">
      <div className="login-card">
        <img src="/brand-logo.png" alt="国民养老保险" className="login-logo" />
        <div className="login-title">新人航行计划</div>
        <div className="login-subtitle">登录开启你的知识海洋之旅</div>
        <form onSubmit={submit}>
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="姓名全拼"
            className="login-field"
          />
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="密码"
            className="login-field"
          />
          {error && <div className="login-error">{error}</div>}
          <button type="submit" disabled={busy} className="btn-primary login-btn">
            {busy ? '登录中…' : '登录'}
          </button>
        </form>
      </div>
    </div>
  );
}
