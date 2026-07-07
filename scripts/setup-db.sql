-- 一次性建库脚本（幂等）。用 postgres 超级用户执行：
--   psql -U postgres -d postgres -f scripts/setup-db.sql
-- 执行后即可用 orientation_app / orientation_dev_pass 连接 orientation 库。
-- 若角色/库已存在，会跳过创建并确保密码与权限正确。

-- 1. 角色 orientation_app：不存在则建，存在则重置密码
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'orientation_app') THEN
    CREATE ROLE orientation_app LOGIN PASSWORD 'orientation_dev_pass';
  ELSE
    ALTER ROLE orientation_app LOGIN PASSWORD 'orientation_dev_pass';
  END IF;
END
$$;

-- 2. 数据库 orientation：不存在则建（属主 orientation_app）
SELECT 'CREATE DATABASE orientation OWNER orientation_app'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'orientation')\gexec

-- 3. 库级权限
GRANT ALL PRIVILEGES ON DATABASE orientation TO orientation_app;

-- 4. 切到 orientation 库，授予 public schema 权限（PG15 起 public 默认不再给 CREATE）
\connect orientation
GRANT ALL ON SCHEMA public TO orientation_app;
