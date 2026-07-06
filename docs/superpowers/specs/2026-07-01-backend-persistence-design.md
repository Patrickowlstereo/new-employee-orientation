# 新员工导引系统 · 后端与数据持久化设计

- 日期：2026-07-01
- 范围：将现有"国民养老·新人航行计划"单文件 Demo 演进为可用的前后端分离系统
- 本轮聚焦：**学习进度持久化 + 管理后台**；积分/兑换/测验成绩不在本轮范围

## 1. 背景与现状

现有项目为纯前端单文件 Demo：

- `index.html`（3220 行）：主游戏，含航行首页/知识地图、海洋航行、知识小岛、测验、文化答题、积分商城、文档面板、视频播放、引导等场景
- `map.html`：交互式中国地图
- `企业文化转盘.html`：企业文化知识转盘
- 多张省份图片 + `index.html.bak` 备份
- 纯 HTML/CSS/JS，无后端、无构建依赖、git 尚无提交记录

Demo 已硬编码 7 大机构（北京、上海、山东、四川、广东、重庆、浙江）、5 个知识小岛（关于公司、入职须知、办公指南、公司产品、知识测验）及约 16 份文档清单。

## 2. 约束（已与用户确认）

| 维度 | 选择 |
|------|------|
| 后端技术栈 | Java（Spring Boot） |
| 数据库 | PostgreSQL |
| 身份认证 | 员工姓名全拼 + 密码登录，JWT |
| 前端策略 | 迁 React（重写，Demo 作视觉参考原型） |
| 持久化范围 | 学习进度 + 管理后台（积分/兑换/测验本轮不做） |
| 部署 | 内网裸机，JVM 进程 + 本地 PostgreSQL + 本地文件系统存文档 |
| 传输安全 | 先 HTTP（内网链路可信前提下），后续加证书 |

## 3. 整体架构

pnpm 前端 workspace + Java 后端模块（前后端异构，根目录统筹）：

```
new-employee-orientation/
├── apps/
│   ├── web/          新人游戏前端（React + Vite + TS + Zustand）
│   └── admin/        管理后台前端（React + Vite + TS + Ant Design）
├── api/              后端（Spring Boot + Spring Data JPA + Java 17，Maven）
├── packages/
│   └── shared/       前端共享类型、常量、枚举、axios 封装
├── uploads/          管理员上传的真实文档（gitignore）
├── pnpm-workspace.yaml
├── pom.xml / api/pom.xml
└── package.json
```

关键决策：

- **前后端异构仓库**：前端两个 React 应用归 pnpm workspace（`apps/`），后端为独立 Maven Java 模块（`api/`）。前端共享类型放 `packages/shared`（仅前端消费，因为 Java 无法直接复用 TS 类型——契约靠 OpenAPI/手写 DTO + 前端类型生成保证一致，见第 5 节）。
- **后端选型**：Spring Boot 3 + Java 17 + Spring Data JPA + PostgreSQL JDBC；JWT 用 `jjwt`；文件上传用 Spring MVC `MultipartFile`；构建用 Maven。
- **`packages/shared` 仍为前端内容模型单一数据源**：机构、岛屿、文档分类等业务常量被 web 与 admin 共用，避免前端两处漂移。后端另有对应 Java 枚举/常量，前后端契约通过 API DTO + 前端类型定义对齐（不自动生成，保持轻量；如后续需要可引入 OpenAPI 生成）。
- **现有单文件 Demo 作为视觉/交互参考原型**：保留（移至 `demo/legacy/`），新版 web 从它迁移视觉与动效，但代码重写，不直接在 3220 行文件上改。
- **单 JVM 进程同时托管 API 与两份前端静态产物**：Spring Boot 打成可执行 jar，配置静态资源映射——`/api/**` 走 Controller，`/admin/**` 映射到 `apps/admin/dist`，其余 `/` 映射到 `apps/web/dist`。java -jar 启动单进程，systemd 或脚本守护。

## 4. 数据模型（PostgreSQL / JPA）

实体（`@Entity`，Spring Data JPA）：

```
User                    用户（新人 + 管理员）
  id, username(姓名全拼,唯一), name, passwordHash, salt,
  role(USER|ADMIN), employeeNo(可选), createdAt

Institution             机构（7条种子：北京/上海/山东/四川/广东/重庆/浙江）
  id, key(唯一短码如'BJ'), name, order

Island                  知识小岛（5个）
  id, key, name, order, institutionId(归属机构)

Doc                     文档（管理员增删改 + 上传真实文件）
  id, title, category, institutionId, islandId, required(必修),
  filePath(uploads下相对路径), fileType(pdf/docx/pptx/xlsx),
  order, active(软删除)

Progress                学习进度（核心持久化，User × Doc）
  id, userId, docId, status(NOT_STARTED|READING|COMPLETED),
  progressPct(0-100), lastReadAt, completedAt,
  唯一(userId,docId)

IslandState             小岛解锁/完成状态（User × Island）
  id, userId, islandId, status(LOCKED|UNLOCKED|COMPLETED),
  唯一(userId,islandId)
```

设计要点：

- **文档级进度为单一真相源**：`Progress`（文档级）是真相，小岛状态由其下文档进度聚合派生。`IslandState` 只记离散的解锁/完成状态，不重复存进度，避免与文档进度打架。聚合逻辑在服务层。
- **现有 Demo 硬编码数据迁入种子**：7 机构、5 岛屿、约 16 份文档清单作为 `data.sql` / `DataInitializer`（`CommandLineRunner`）初始数据，文件名占位由管理员后台上传真实文件覆盖。
- **积分/兑换/测验本轮不建表**：按确认范围聚焦学习进度与管理后台，避免范围蔓延；后续若做再加 `Point`/`QuizResult` 等表。
- **`active` 软删除**：管理员下架文档不真删，保留历史进度可追溯。
- **uploads 文件不进库**：`Doc.filePath` 只存相对路径，文件在本地 `uploads/`，下载经 API 鉴权后流式返回，不直接暴露静态目录。
- **账号开通**：管理员后台预建账号，录入姓名 → 系统生成全拼 `username`（重名自动加数字后缀，如 `zhangwei2`）→ 设初始密码 → 交员工；员工首次登录后可改密码。

## 5. 后端 API 设计（Spring Boot + JPA）

### 身份认证

- `POST /api/auth/login { username, password }` → 校验 BCrypt → 签发 JWT（HS256，secret 在 `application.yml`/环境变量），返回 `{ token, user }`
- `GET /api/auth/me` → 当前用户
- `PUT /api/auth/password { oldPassword, newPassword }` → 改密（验证旧密码）
- JWT 内容：`{ userId, username, name, role }`；过期时间 8h（可配）；用 `jjwt` 签发/解析
- 安全：登录失败不区分"用户不存在/密码错误"（防枚举）；密码最小长度 8 位；BCrypt cost factor 12
- 拦截：JWT 过滤器（`OncePerRequestFilter`）校验 token 并填充 `SecurityContext`；管理后台路径 `/api/admin/**` 用 Spring Security `hasRole('ADMIN')` 鉴权

### 内容（新人 + 后台共用读取）

```
GET  /api/institutions        机构列表（含小岛）
GET  /api/islands             小岛列表（可按机构筛）
GET  /api/docs                文档列表（按机构/小岛/必修筛选）
GET  /api/docs/{id}           文档元信息
GET  /api/docs/{id}/file      鉴权后流式下载真实文件
```

### 学习进度（新人调用）

```
GET  /api/progress            当前用户全部进度 + 小岛状态聚合
PUT  /api/progress/{docId}    上报/更新单文档进度 {status, progressPct}
POST /api/progress/{docId}/complete  标记完成
```

### 后台管理（仅 ADMIN）

```
CRUD /api/admin/users             用户列表/角色管理/建账号/重置密码
CRUD /api/admin/institutions      机构配置
CRUD /api/admin/islands           小岛配置
CRUD /api/admin/docs              文档增删改 + 上下架/设必修
POST /api/admin/docs/{id}/upload  MultipartFile 上传→存 uploads/→更新filePath
GET  /api/admin/stats             全员学习数据统计（按机构/小岛完成率）
```

设计要点：

- **进度上报幂等**：`PUT /api/progress/{docId}` 以 `(userId, docId)` 唯一约束 upsert（JPA `find` 后 `save` 或原生 upsert）；服务层用 `progressPct` 单调递增逻辑（不回退，避免刷新覆盖已读进度）。
- **小岛状态服务端聚合**：`GET /api/progress` 响应同时返回 `documents[]`（每文档进度）与 `islands[]`（每小岛聚合状态 + 完成率），前端无需二次计算；解锁/完成判定规则在此固化（如"必修文档全完成则小岛完成"），单一真相。
- **文件上传**：Spring MVC `MultipartFile` 接收，存 `uploads/<docId>-<原文件名>`，限制大小（50MB，`spring.servlet.multipart.max-file-size`）、白名单扩展名（pdf/docx/pptx/xlsx），防任意上传。
- **文件下载鉴权**：`/api/docs/{id}/file` 校验登录后用 `StreamingResponseBody` 流式返回；不把 `uploads/` 挂成公开静态目录。
- **错误约定**：`@RestControllerAdvice` 统一 `{code, message}`，语义化状态码（400/401/403/404/409/500）。
- **校验**：Jakarta Validation（`@Valid` + Bean Validation 注解）校验请求体；DTO 与前端 `packages/shared` 类型手动对齐契约（轻量，不自动生成）。
- **统计接口**：一条聚合 SQL（JPQL 或原生 SQL）按机构/小岛分组算完成率，避免 N+1。

## 6. 前端设计

### apps/web（新人游戏前端）

- 栈：React 18 + Vite + TypeScript + Zustand + React Router
- 从 Demo 迁移视觉与 CSS 动效（波浪、船只、小岛、金币飞入、飓风等 keyframes）为 React 组件 + CSS Modules，保留游戏化观感；`企业文化转盘.html`、`map.html` 作为独立路由迁入
- 路由：
  - `/login` 登录页
  - `/` 航行首页 / 知识地图（7 机构）
  - `/voyage/:instKey` 海洋航行 + 5 小岛
  - `/island/:islandKey` 小岛文档列表
  - `/doc/:docId` 文档阅读面板（含阅读进度条）
  - `/culture-wheel` 企业文化转盘
  - `/map` 中国地图
- 状态（Zustand）：
  - `authStore`：token、当前用户，持久化到 localStorage
  - `progressStore`：进度与小岛聚合状态，进入页面时 `GET /api/progress` 拉取；阅读时本地乐观更新 + 节流上报（每 10s 或进度变化 ≥5% 上报一次）
- 文档阅读进度上报：滚动/停留时长驱动 `progressPct`，离开页面强制上报最终值；后端幂等 upsert + 单调不回退兜底
- 文件下载：点"打开"→ 带 token 请求 `/api/docs/:id/file`，blob 触发下载或新窗口预览 PDF，不暴露文件 URL

### apps/admin（管理后台前端）

- 栈：React 18 + Vite + TypeScript + Ant Design（ProTable/ProForm）+ React Router
- 路由：
  - `/login` 管理员登录（同一套 JWT，role=ADMIN 才能进）
  - `/dashboard` 全员学习统计概览
  - `/users` 用户与账号管理（建账号、重置密码、改角色）
  - `/institutions` 机构配置
  - `/islands` 小岛配置
  - `/docs` 文档管理（增删改 + 上传文件、设必修、上下架）
- 权限：登录后校验 `role=ADMIN`，非管理员重定向回登录

### packages/shared（仅前端消费）

- 共享 TS 类型：`User`、`Institution`、`Island`、`Doc`、`Progress`、`IslandState`、API 请求/响应 DTO、枚举（`DocStatus`、`IslandStatus`、`UserRole`）——与后端 Java DTO 手动对齐契约
- 共享常量：机构 key 列表、小岛定义、文档分类（前端 web 与 admin 共用）
- 共享 `axios` 实例封装：自动附 `Authorization`、401 重定向登录

设计要点：

- web 与 admin 物理隔离，独立构建、独立路由，互不污染包体积与 UI 风格
- token 存 localStorage，Axios 拦截器自动附 `Authorization` + 401 重定向登录
- 进度乐观更新 + 节流上报 + 离开强制上报；后端单调不回退保证数据正确
- Demo 视觉迁移而非重设计，沿用现有动画与配色（`:root` 变量原样搬）

## 7. 部署、安全与测试

### 部署（内网裸机）

- 单 JVM 进程：`api` Spring Boot 可执行 jar 同时托管 `/api/**` 与两份前端构建产物（静态资源映射），systemd 或启动脚本守护
- PostgreSQL 本地实例，JPA `hibernate.ddl-auto` 初期 `update`/`validate` + Flyway 管理 schema 迁移
- `uploads/` 本地目录，JVM 启动用户可读写
- 传输：先 HTTP（内网链路可信前提下），后续加证书

### 安全

- 密码 BCrypt 哈希、登录失败不区分用户/密码错误、JWT 过期 + 刷新
- 文件上传白名单 + 大小限制；下载走鉴权流式，不公开静态目录
- `application.yml` + 环境变量管 JWT secret / DB 连接串，敏感配置不进 git

### 测试

- 后端：JUnit 5 + Spring Boot Test，关键服务层单测（进度 upsert 单调不回退、小岛聚合、鉴权、文件下载权限）+ `@SpringBootTest` 集成测（Testcontainers PostgreSQL 或事务回滚隔离）
- 前端：组件交互单测（Vitest + Testing Library），进度上报节流逻辑单测
- 覆盖"进度正确性 + 鉴权 + 文件权限"三条核心链路即可，不追求全覆盖

## 8. 落地顺序

每阶段可独立验收：

1. **脚手架**：根目录 pnpm workspace（前端）+ `api/` Maven Spring Boot 骨架 + JPA 实体 + 种子数据（7 机构/5 岛屿/16 文档占位）
2. **认证**：登录（姓名全拼+密码）+ JWT（jjwt）+ 改密；DataInitializer 种子一个 admin 账号
3. **内容 + 进度**：内容读取 API + 进度上报/聚合 API + web 登录页与航行/小岛/文档页（迁移 Demo 视觉）跑通学习闭环
4. **文件上传下载**：后台文档管理 + 上传（MultipartFile）+ 鉴权下载
5. **管理后台完善**：用户/机构/小岛管理 + 统计概览
6. **打磨**：动画迁移收尾、企业文化转盘/地图迁入、响应式

## 9. 遗留 Demo 处理

现有 `index.html`、`map.html`、`企业文化转盘.html` 等 Demo 文件移至 `demo/legacy/`（独立目录或 git 分支），作为视觉参考；新版不从它改起。
