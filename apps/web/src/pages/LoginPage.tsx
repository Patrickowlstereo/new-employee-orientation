import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
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
    } catch {
      setError('账号或密码错误');
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
        <p className="login-hint">试点账号 admin / admin12345</p>
      </div>
    </div>
  );
}
