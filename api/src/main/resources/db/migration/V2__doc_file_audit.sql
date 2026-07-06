-- 阶段 4：文档上传审计 + 文件类型扩容
-- 1) 记录每份文件由谁、何时上传，满足"可追溯"
-- 2) file_type 扩容以容纳图片/视频等更长的扩展名（如 webm、jpeg）

ALTER TABLE docs ADD COLUMN file_uploaded_at TIMESTAMPTZ;
ALTER TABLE docs ADD COLUMN file_uploaded_by BIGINT REFERENCES users(id);
ALTER TABLE docs ALTER COLUMN file_type TYPE VARCHAR(16);
