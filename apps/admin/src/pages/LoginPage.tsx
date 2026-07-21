import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, Form, Input, Button, message } from 'antd';
import axios from 'axios';
import { useAuthStore } from '../stores/authStore';

export default function LoginPage() {
  const [busy, setBusy] = useState(false);
  const login = useAuthStore((s) => s.login);
  const navigate = useNavigate();

  const onFinish = async (v: { username: string; password: string }) => {
    setBusy(true);
    try {
      await login({ username: v.username.trim(), password: v.password });
      navigate('/admin/', { replace: true });
    } catch (err) {
      // 401 = 凭证错误或权限不足;429 = 防爆破锁定,透传后端提示;其余视为服务不可用
      if (axios.isAxiosError(err) && err.response?.status === 401) {
        message.error('账号或密码错误，或非管理员');
      } else if (axios.isAxiosError(err) && err.response?.status === 429) {
        message.error((err.response.data as { message?: string })?.message ?? '尝试次数过多，请稍后再试');
      } else {
        message.error('服务暂时不可用，请稍后再试');
      }
    } finally { setBusy(false); }
  };

  return (
    <div style={{ display: 'flex', justifyContent: 'center', padding: 80 }}>
      <Card title="管理后台登录" style={{ width: 360 }}>
        <Form onFinish={onFinish}>
          <Form.Item name="username" rules={[{ required: true, message: '请输入姓名全拼' }]}>
            <Input placeholder="姓名全拼" />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password placeholder="密码" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={busy}>登录</Button>
        </Form>
      </Card>
    </div>
  );
}
