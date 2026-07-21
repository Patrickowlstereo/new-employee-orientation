-- 阶段 7 增补:按 Repository 实际查询补二级索引
-- docs: 按小岛/机构过滤与排序(findByIslandId..., countByIslandId, 管理端按机构排序)
-- progress: 按用户聚合(findByUserId)与按文档统计(countByDocId)
-- island_states: 按用户查询(findByUserId)
-- islands: 按机构查询(findByInstitutionId..., countByInstitutionId)
-- users: 按角色过滤(findByRole,管理端统计)
-- 幂等:IF NOT EXISTS,重复执行安全。

CREATE INDEX IF NOT EXISTS idx_docs_island_id       ON docs (island_id);
CREATE INDEX IF NOT EXISTS idx_docs_institution_id  ON docs (institution_id);
CREATE INDEX IF NOT EXISTS idx_progress_doc_id      ON progress (doc_id);
CREATE INDEX IF NOT EXISTS idx_progress_user_id     ON progress (user_id);
CREATE INDEX IF NOT EXISTS idx_island_states_user_id ON island_states (user_id);
CREATE INDEX IF NOT EXISTS idx_islands_institution_id ON islands (institution_id);
CREATE INDEX IF NOT EXISTS idx_users_role           ON users (role);
