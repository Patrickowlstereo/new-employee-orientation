/** 加载失败提示 + 重试按钮,各页面统一的轻量错误态。 */
export default function LoadError({
  message,
  onRetry,
  light = false,
}: {
  message: string;
  onRetry: () => void;
  /** 深色背景页面(如海洋页)用浅色文字 */
  light?: boolean;
}) {
  return (
    <div style={{ color: light ? '#fff' : 'red', padding: 16, textAlign: 'center' }}>
      {message}
      <button
        className="btn-secondary"
        style={{ marginLeft: 12, padding: '4px 16px', width: 'auto' }}
        onClick={onRetry}
      >
        重试
      </button>
    </div>
  );
}
