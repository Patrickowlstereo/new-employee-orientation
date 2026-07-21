import axios from 'axios';

/** 区分网络/服务不可用与接口返回错误,给用户可理解的加载失败文案。 */
export function describeLoadError(err: unknown): string {
  if (axios.isAxiosError(err)) {
    if (err.response) {
      return `服务返回错误（${err.response.status}），请稍后重试`;
    }
    return '网络异常或服务不可用，请检查网络后重试';
  }
  return '加载失败，请重试';
}
