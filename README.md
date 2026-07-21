# 国民养老 · 新人航行计划

新人入职知识学习系统：员工登录后按"机构 → 小岛 → 文档"路径学习，进度自动保存；管理员在独立后台自助维护内容并查看全员学习统计；学习端为航海视觉（中国地图首页 + 海洋小岛），文档支持在线预览与大视频流式播放。

> 业务汇报见 [`docs/项目优化工作总结.md`](docs/项目优化工作总结.md)；技术设计与实现计划见 `docs/superpowers/`。

## 技术栈

- 后端：Java 17 + Spring Boot 3.2 + JPA + Flyway + PostgreSQL 15
- 前端：React 18 + TypeScript + Vite 5（学习端 `apps/web`、管理后台 `apps/admin`）
- 共享类型：`packages/shared`
- 部署：单体打包，后端托管三端（学习页 / 管理后台 / 数据接口），内网裸机运行

## 目录结构

```
api/            后端服务（Spring Boot）
apps/web/       学习端前端（员工使用）
apps/admin/     管理后台前端（管理员使用）
packages/shared/ 前后端共享类型与常量
docs/           业务汇报、技术设计、实现计划
uploads/        上传文件存储目录（运行时生成，不入库）
```

## 开发运行

1. 安装 JDK 17、Maven、PostgreSQL 15+、Node 20+、pnpm 9。
2. 建库：
   ```sql
   CREATE DATABASE orientation;
   CREATE USER orientation_app WITH PASSWORD 'orientation_dev_pass';
   GRANT ALL PRIVILEGES ON DATABASE orientation TO orientation_app;
   ```
3. 复制 `api/src/main/resources/application-local.yml.example` 为 `application-local.yml`，按需改密码（含本地 JWT 密钥与 Swagger 开关）。
4. 启动后端：`cd api && mvn spring-boot:run -Dspring-boot.run.profiles=local`（或设置 `SPRING_PROFILES_ACTIVE=local`；Flyway 自动建表 + 注入种子数据。初始 `admin` 密码取环境变量 `ADMIN_INITIAL_PASSWORD`，未设置时随机生成并在启动日志中打印一次）。
5. 前端开发：`pnpm install`，然后 `pnpm dev:web`（学习端，5173）或 `pnpm dev:admin`（管理后台，5174）。

Swagger UI 默认关闭：本地由 `application-local.yml` 开启（`/swagger-ui/index.html`）；其他环境确有需要时设置 `SWAGGER_ENABLED=true`。

## 生产打包

```bash
pnpm -r build                                       # 构建前后端
cp -r apps/web/dist  api/dist/web                   # 拷贝学习端产物
cp -r apps/admin/dist api/dist/admin                # 拷贝管理后台产物
cd api && mvn package                                # 打包单体 jar
java -jar api/target/orientation-api-0.1.0.jar      # 单进程对外服务
```

启动后：学习端在根路径 `/`，管理后台在 `/admin/`。生产环境必须通过环境变量提供 `JWT_SECRET`（≥32 字节），初始 `admin` 密码由 `ADMIN_INITIAL_PASSWORD` 指定（未设置时随机生成并打印一次，请立即登录修改）。
