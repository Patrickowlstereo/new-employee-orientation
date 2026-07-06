import type { FileCategory } from './types';

// 7 大机构短码，与后端 DataInitializer 种子一致
export const INSTITUTION_KEYS = [
  'BJ', 'SH', 'SD', 'SC', 'GD', 'CQ', 'ZJ',
] as const;

export const API_BASE = '/api';

// 文件类型白名单（与后端 FileTypeSupport 一致）。后台上传 accept 直接复用。
export const FILE_EXTENSION_GROUPS: Record<Exclude<FileCategory, 'OTHER'>, readonly string[]> = {
  DOCUMENT: ['pdf', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx', 'txt', 'csv'],
  IMAGE: ['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp', 'svg', 'ico'],
  VIDEO: ['mp4', 'webm', 'mov', 'mkv', 'avi', 'flv', 'm4v'],
  AUDIO: ['mp3', 'wav', 'aac', 'm4a', 'ogg', 'flac'],
  ARCHIVE: ['zip', 'rar', '7z'],
};

export const ALLOWED_FILE_EXTENSIONS: readonly string[] = [
  ...FILE_EXTENSION_GROUPS.DOCUMENT,
  ...FILE_EXTENSION_GROUPS.IMAGE,
  ...FILE_EXTENSION_GROUPS.VIDEO,
  ...FILE_EXTENSION_GROUPS.AUDIO,
  ...FILE_EXTENSION_GROUPS.ARCHIVE,
];

// antd Upload 的 accept 属性：.pdf,.docx,...
export const UPLOAD_ACCEPT = ALLOWED_FILE_EXTENSIONS.map((e) => `.${e}`).join(',');

const EXT_TO_CATEGORY: ReadonlyMap<string, FileCategory> = (() => {
  const m = new Map<string, FileCategory>();
  (Object.keys(FILE_EXTENSION_GROUPS) as Array<Exclude<FileCategory, 'OTHER'>>).forEach((cat) => {
    FILE_EXTENSION_GROUPS[cat].forEach((ext) => m.set(ext, cat));
  });
  return m;
})();

/** 取小写无点扩展名 */
export function extensionOf(filename: string | null | undefined): string | null {
  if (!filename) return null;
  const dot = filename.lastIndexOf('.');
  if (dot < 0 || dot === filename.length - 1) return null;
  return filename.substring(dot + 1).toLowerCase();
}

export function categorizeFileType(ext: string | null | undefined): FileCategory {
  if (!ext) return 'OTHER';
  return EXT_TO_CATEGORY.get(ext.toLowerCase()) ?? 'OTHER';
}

export const FILE_CATEGORY_LABELS: Record<FileCategory, string> = {
  DOCUMENT: '文档',
  IMAGE: '图片',
  VIDEO: '视频',
  AUDIO: '音频',
  ARCHIVE: '压缩包',
  OTHER: '其他',
};

/** 机构在中国地图首页的浮动定位与展示信息(键 = Institution.key)。 */
export interface InstitutionGeo {
  top: string;   // 相对地图容器的百分比
  left: string;
  emoji: string;
  label: string;
  img: string;   // /institutions/xxx.png
}

export const INSTITUTION_GEO: Record<string, InstitutionGeo> = {
  BJ: { top: '10%', left: '60%', emoji: '🏛️', label: '北京(总部)', img: '/institutions/beijing.png' },
  SD: { top: '22%', left: '66%', emoji: '⛰️', label: '山东', img: '/institutions/shandong.png' },
  SH: { top: '43%', left: '76%', emoji: '🏙️', label: '上海', img: '/institutions/shanghai.png' },
  ZJ: { top: '52%', left: '74%', emoji: '🌊', label: '浙江', img: '/institutions/zhejiang.png' },
  GD: { top: '70%', left: '64%', emoji: '🌴', label: '广东', img: '/institutions/guangdong.png' },
  SC: { top: '50%', left: '35%', emoji: '🐼', label: '四川', img: '/institutions/sichuan.png' },
  CQ: { top: '47%', left: '47%', emoji: '🌆', label: '重庆', img: '/institutions/chongqing.png' },
};

/** 兜底:未在 INSTITUTION_GEO 中的机构居中展示。 */
export const INSTITUTION_GEO_FALLBACK: InstitutionGeo = {
  top: '45%', left: '50%', emoji: '🏢', label: '', img: '',
};
