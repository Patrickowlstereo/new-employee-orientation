# 阶段 6:航海视觉迁移 + 大视频流式预览

## 目标
把原静态 Demo 的航海视觉(中国地图机构入口 + 海洋小岛航行)迁移进 React 学习端,并把文档预览从「整文件入内存 blob」改为「Range 流式 + token 直链」,使大视频可拖动进度条、分片加载。

仅作用于学习端 `apps/web`,后台 `apps/admin` 保持 antd 功能型不动。

## 决策(已与用户确认)
1. 首页结构:**地图 → 海洋 两级**。`/` = 中国地图+机构卡片(map.html 风格);新增 `/institution/:id` = 海洋+小岛+SVG 航线(index.html voyageScreen 风格);`/island/:id` 文档列表不变(仅复用设计令牌美化)。
2. 视觉保真度:**精炼子集**。渐变海面 + 浮动小岛 + 发光环 + SVG 航线 + 锁定/进度徽章 + 少量星光。不做鼠标涟漪/多层涌浪/caustic 光纹(重且 React 内脆弱)。
3. 流式预览:**token 入 URL + Range**。后端 `/api/docs/{id}/file` 支持 Range(206),仅该路径允许 `?token=` 透传 JWT;前端媒体元素直链,原生支持 seek/分片。

---

## 一、后端:文件接口 Range + token 透传

### 1. `ContentController.downloadFile` 重写(支持 Range)
- 入参加 `@RequestHeader(value="Range", required=false) String rangeHeader`。
- 无 Range → 200,`Accept-Ranges: bytes`,`Content-Disposition: inline`(媒体内联),`Content-Length=total`,`FileSystemResource`。
- 有 Range → 用 `HttpRange.parseRanges` + `ranges.get(0).toResourceRegion(resource)` 得 `ResourceRegion`,返回 206,带 `Content-Disposition: inline`、`contentType`。转换器自动写 `Content-Range`/`Content-Length`。
- `Content-Disposition` 由原 `attachment` 改 `inline`(预览内联;DocPage 下载按钮仍走 blob + `a.download`,不受影响)。
- 404 检查(filePath 空 / 文件不存在)逻辑保留。
- `FileSystemResource` 路径仍走 `DocFileStorage.resolve` 做穿越防护(目前 downloadFile 用 `new File(uploadsDir, filePath)`,顺手改为 `storage.resolve(filePath).toFile()` 统一防护)。

### 2. `JwtAuthFilter` 支持查询参数 token(仅文件接口)
- 现状只读 `Authorization: Bearer`。改为:header 缺失时,若 URI 形如 `/api/docs/<id>/file`,则取 `req.getParameter("token")` 作 token。
- 注释说明:仅文件接口放行 query token,因为 `<video>/<img>/<iframe>` 无法附加 Authorization 头;token 入 URL 有日志暴露风险,内部工具可接受,且范围受限。

### 3. 测试
- 新增 `ContentControllerTest`(`@WebMvcTest(ContentController.class)`,不 @Import SecurityConfig → 关鉴权,聚焦 Range 逻辑):
  - `无Range返回200且带AcceptRanges`
  - `有Range返回206且带ContentRange`
  - `文件不存在返回404`
  - 用临时文件写已知字节,mock `ContentService.getDocEntity` 返回指向该文件的 Doc。`@Import(GlobalExceptionHandler.class)` 保证 404 透传。
- `SecurityConfigTest` 增补:controllers 加 `ContentController`,加 `@MockBean ContentService contentService` + `DocFileStorage`。新增:
  - `fileEndpointWithoutTokenReturns401`
  - `fileEndpointWithQueryTokenPassesAuth`:mock `jwtService.parse("tok")` 返回带 subject/role 的 Claims;mock `contentService.getDocEntity` 抛 `ResponseStatusException(NOT_FOUND)`;断言 `?token=tok` → 404(非 401 即证明鉴权通过)。

---

## 二、前端:设计系统 + 页面迁移

### 1. 设计令牌
- 新增 `apps/web/src/styles/tokens.css`:从 Demo 抽取 CSS 变量(蓝/紫/玫瑰/翠/琥珀/slate 色阶、radius、shadow、font、transition)与精炼动画(`float`/`bannerFloat`/`pulse`/`sparkle`)。
- `apps/web/src/main.tsx` 顶部 `import './styles/tokens.css'`。
- `apps/web/index.html` 加 `<link rel="icon" href="/favicon.svg">`。

### 2. 静态资源(图片)
- 拷贝并改 ASCII 名进 `apps/web/public/`(git 按内容去重,不增仓库体积):
  - `中国地图.png` → `public/china-map.png`
  - `北京/上海/山东/四川/广州/重庆/浙江.png` → `public/institutions/{beijing,shanghai,shandong,sichuan,guangdong,chongqing,zhejiang}.png`
  - `brand-logo.png` → `public/brand-logo.png`;`favicon.svg` → `public/favicon.svg`

### 3. shared 常量:机构地理信息
- `packages/shared/src/constants.ts` 增 `INSTITUTION_GEO`:按 `institution.key`(BJ/SH/SD/SC/GD/CQ/ZJ)映射 `{top%, left%, emoji, label, img}`,值取自 map.html 的 `home-inst-*` 定位 + 卡片 emoji/标签。未知 key 走兜底居中。

### 4. `VoyagePage`(首页:中国地图)
- 渐变背景 + 顶部导航(logo / 用户名 / 退出)。
- 容器内 `<img src="/china-map.png">` 为底图,其上按 `INSTITUTION_GEO[key]` 绝对定位浮动机构卡片(城市照片 + emoji + 名称 + 完成度小字),`bannerFloat` 动画,hover 放大。
- 点击机构 → `navigate(/institution/:id)`。
- 数据:`fetchInstitutions()`(含 islands)+ progress aggregate 算每机构完成度(已完成小岛数/总小岛数)。
- 响应式:窄屏缩放容器(参照 map.html media query)。

### 5. 新增 `InstitutionPage`(`/institution/:id`:海洋小岛)
- 海洋渐变背景 + 精炼星光层(CSS,少量)。
- 顶部「← 返回」+ 机构标题。
- 小岛区:取该机构 islands,按 order 沿弧线定位(位置由 index 计算:`x=(i+1)/(n+1)*100%`,`y=50% + sin*offset`),岛间 SVG 航线连接(consecutive)。
- 每岛:发光环 + 名称 + 状态徽章(🔒 LOCKED / 🔓 UNLOCKED / ✅ COMPLETED)+ 进度 `completed/total`。LOCKED 岛 `pointer-events:none` + 灰度。
- 点击 UNLOCKED/COMPLETED 岛 → `/island/:id`。
- 数据:`fetchInstitutions()` 找到目标机构(小列表,不必加单查接口),其 islands 与 progress aggregate 按 islandId 交叉。
- 路由:`App.tsx` 加 `<Route path="/institution/:institutionId" ...>`。

### 6. `IslandPage` / `DocPage` 美化
- 套用 tokens.css 变量(卡片、进度条、按钮),保持现有功能与滚动进度逻辑不变。

### 7. `DocPage` 流式预览改造
- `api/content.ts` 增 `docStreamUrl(docId) = /api/docs/${docId}/file?token=${localStorage.token}`。
- `renderPreview`:IMAGE/VIDEO/AUDIO/PDF 改用 `docStreamUrl` 直链(`<img src>` / `<video src controls>` / `<audio src controls>` / `<iframe src>`),删除 blob 拉取 effect、`previewUrl`/`previewError`/`URL.createObjectURL` 相关逻辑。
- 下载按钮保留(blob + `a.download`)。
- 注释更新:整文件 blob 已移除,大视频走 Range 流式。

---

## 三、验证
- 后端:`mvn -f api/pom.xml test`(新增 ContentControllerTest + SecurityConfigTest 增补)。
- 前端:`pnpm --filter @gmnl/web build`(tsc + vite),确保类型与构建通过。
- (DB/8080 未起,活体冒烟留待用户;以自动化测试 + 构建为保证。)

## 四、提交
完成后单条 commit:`feat: 阶段6 航海视觉迁移与视频流式预览`。
