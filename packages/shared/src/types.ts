// 本文件的 DTO/请求/响应类型由后端 OpenAPI 规范(packages/shared/openapi.json)派生,
// 勿手改生成产物 src/generated/api-types.ts。改后端 DTO 后:
//   1) mvn -f api/pom.xml test -Dtest=OpenApiSpecWriterTest -Ddump.openapi=true   (重导出 openapi.json)
//   2) pnpm --filter @gmnl/shared gen:api                                          (重生成 api-types.ts)
//   3) 提交 openapi.json 与 api-types.ts。pnpm check:api 可校验 api-types.ts 是否最新。
import type { components } from './generated/api-types';

type Schemas = components['schemas'];

/**
 * 深度去可选:递归把所有属性变为必填(去掉 ? 与隐式 undefined),保留显式 | null,
 * 并递归进数组元素与嵌套对象。
 *
 * <p>springdoc 默认把无 @NotNull 的响应字段标为可选(因 record 字段无校验注解),
 * 但 Jackson 总是序列化 record 的全部字段(值可为 null),故用 DeepReq 还原"全必填"契约,
 * 与历史 hand-written types.ts 形态一致,前端消费零改动。
 */
type DeepReq<T> =
  T extends (infer U)[]
    ? DeepReq<U>[]
    : T extends object
      ? { [K in keyof T]-?: DeepReq<Exclude<T[K], undefined>> }
      : T;

// ===== 枚举/状态(由规范内联枚举派生) =====
export type UserRole = DeepReq<Schemas['UserDto']>['role'];
export type DocStatus = DeepReq<Schemas['ProgressItemDto']>['status'];
export type IslandStatus = DeepReq<Schemas['IslandStateViewDto']>['status'];
// 文件大类,与后端 FileTypeSupport.Category 对应(AdminDocDto.fileCategory 用枚举类型,故可派生)
export type FileCategory = DeepReq<Schemas['AdminDocDto']>['fileCategory'];

// 文件扩展名(小写、无点)。阶段 4 起支持文档/图片/视频/音频/压缩等全面类型,故放宽为 string
// (规范里 fileType 即 string,非枚举)。
export type DocFileType = string;

// ===== 领域视图 =====
export type User = DeepReq<Schemas['UserDto']>;
export type Institution = DeepReq<Schemas['InstitutionDto']>;
export type Island = DeepReq<Schemas['IslandDto']>;
export type Doc = DeepReq<Schemas['DocDto']>;
export type AdminDoc = DeepReq<Schemas['AdminDocDto']>;

// ===== 学习进度 =====
export type ProgressItem = DeepReq<Schemas['ProgressItemDto']>;
export type IslandStateView = DeepReq<Schemas['IslandStateViewDto']>;
export type ProgressAggregate = DeepReq<Schemas['ProgressAggregateDto']>;

// ===== 请求/响应 DTO =====
export type LoginRequest = DeepReq<Schemas['LoginRequest']>;
export type LoginResponse = DeepReq<Schemas['LoginResult']>;
export type ChangePasswordRequest = DeepReq<Schemas['ChangePasswordRequest']>;
export type UpsertProgressRequest = DeepReq<Schemas['UpsertProgressRequest']>;

// ===== 后台内容 CRUD 请求 =====
export type InstitutionUpsertRequest = DeepReq<Schemas['InstitutionUpsertRequest']>;
export type IslandUpsertRequest = DeepReq<Schemas['IslandUpsertRequest']>;
export type DocUpsertRequest = DeepReq<Schemas['DocUpsertRequest']>;

// ===== 全员学习统计 =====
export type UserStats = DeepReq<Schemas['UserStatsDto']>;
export type IslandCompletion = DeepReq<Schemas['IslandCompletionDto']>;
export type StatsOverview = DeepReq<Schemas['StatsOverviewDto']>;
