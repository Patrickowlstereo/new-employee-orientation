-- 阶段 6 增补：企业文化转盘作为"关于公司"小岛的互动学习材料
-- fileType='html' 标记为互动模块，filePath 指向学习端 public 静态资源（非上传文件）；
-- 学习端 DocPage 对 html 类型用沙箱 iframe 渲染 /{filePath}。
-- 幂等：已存在同名文档则跳过。

INSERT INTO docs (title, category, institution_id, island_id, required, file_path, file_type, "order", active)
SELECT '企业文化转盘', '关于公司', i.institution_id, i.id, TRUE, 'culture-wheel/culture-wheel.html', 'html', 1, TRUE
FROM islands i
WHERE i.key LIKE 'about\_%'
  AND NOT EXISTS (
    SELECT 1 FROM docs d WHERE d.island_id = i.id AND d.title = '企业文化转盘'
  );
