package com.gmnl.orientation.content;

import org.springframework.http.MediaType;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 文件类型支持：扩展名白名单（文档/图片/视频/音频/压缩）、分类、MIME 推断、文件名净化。
 *
 * <p>白名单按业务分类组织，便于后续增补。MIME 仅需保证浏览器内联预览所需类型（图片/视频/音频/PDF）
 * 正确即可；office 等仅用于下载的类型回退 octet-stream 不影响体验。
 */
public final class FileTypeSupport {

  public enum Category { DOCUMENT, IMAGE, VIDEO, AUDIO, ARCHIVE, OTHER }

  private static final Set<String> DOCUMENT = Set.of("pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "csv");
  private static final Set<String> IMAGE   = Set.of("png", "jpg", "jpeg", "gif", "webp", "bmp", "svg", "ico");
  private static final Set<String> VIDEO   = Set.of("mp4", "webm", "mov", "mkv", "avi", "flv", "m4v");
  private static final Set<String> AUDIO   = Set.of("mp3", "wav", "aac", "m4a", "ogg", "flac");
  private static final Set<String> ARCHIVE = Set.of("zip", "rar", "7z");

  /** 预览所需类型的 MIME；未命中回退 octet-stream。 */
  private static final Map<String, MediaType> MIME = Map.ofEntries(
      Map.entry("pdf", MediaType.APPLICATION_PDF),
      Map.entry("png", MediaType.IMAGE_PNG),
      Map.entry("jpg", MediaType.IMAGE_JPEG),
      Map.entry("jpeg", MediaType.IMAGE_JPEG),
      Map.entry("gif", MediaType.IMAGE_GIF),
      Map.entry("webp", MediaType.parseMediaType("image/webp")),
      Map.entry("bmp", MediaType.parseMediaType("image/bmp")),
      Map.entry("svg", MediaType.parseMediaType("image/svg+xml")),
      Map.entry("ico", MediaType.parseMediaType("image/x-icon")),
      Map.entry("mp4", MediaType.parseMediaType("video/mp4")),
      Map.entry("webm", MediaType.parseMediaType("video/webm")),
      Map.entry("mov", MediaType.parseMediaType("video/quicktime")),
      Map.entry("mkv", MediaType.parseMediaType("video/x-matroska")),
      Map.entry("avi", MediaType.parseMediaType("video/x-msvideo")),
      Map.entry("flv", MediaType.parseMediaType("video/x-flv")),
      Map.entry("m4v", MediaType.parseMediaType("video/x-m4v")),
      Map.entry("mp3", MediaType.parseMediaType("audio/mpeg")),
      Map.entry("wav", MediaType.parseMediaType("audio/wav")),
      Map.entry("aac", MediaType.parseMediaType("audio/aac")),
      Map.entry("m4a", MediaType.parseMediaType("audio/mp4")),
      Map.entry("ogg", MediaType.parseMediaType("audio/ogg")),
      Map.entry("flac", MediaType.parseMediaType("audio/flac")));

  private FileTypeSupport() {}

  /** 取小写无点扩展名；无扩展名返回 null。 */
  public static String extensionOf(String filename) {
    if (filename == null) return null;
    int dot = filename.lastIndexOf('.');
    if (dot < 0 || dot == filename.length() - 1) return null;
    return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
  }

  public static boolean isAllowed(String ext) {
    if (ext == null) return false;
    String e = ext.toLowerCase(Locale.ROOT);
    return DOCUMENT.contains(e) || IMAGE.contains(e) || VIDEO.contains(e)
        || AUDIO.contains(e) || ARCHIVE.contains(e);
  }

  public static Category categoryOf(String ext) {
    if (ext == null) return Category.OTHER;
    String e = ext.toLowerCase(Locale.ROOT);
    if (DOCUMENT.contains(e)) return Category.DOCUMENT;
    if (IMAGE.contains(e))   return Category.IMAGE;
    if (VIDEO.contains(e))   return Category.VIDEO;
    if (AUDIO.contains(e))   return Category.AUDIO;
    if (ARCHIVE.contains(e)) return Category.ARCHIVE;
    return Category.OTHER;
  }

  public static MediaType mediaTypeOf(String ext) {
    if (ext == null) return MediaType.APPLICATION_OCTET_STREAM;
    MediaType mt = MIME.get(ext.toLowerCase(Locale.ROOT));
    return mt != null ? mt : MediaType.APPLICATION_OCTET_STREAM;
  }

  /**
   * 净化原始文件名：仅保留字母、数字、点、下划线、连字符；其余替换为下划线；
   * 折叠连续下划线并裁剪长度，避免路径穿越与非法字符。
   */
  public static String sanitizeFilename(String raw) {
    if (raw == null || raw.isBlank()) return "file";
    String name = raw.trim();
    StringBuilder sb = new StringBuilder(name.length());
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
          || (c >= '0' && c <= '9') || c == '.' || c == '_' || c == '-') {
        sb.append(c);
      } else {
        sb.append('_');
      }
    }
    String result = sb.toString().replaceAll("_+", "_");
    if (result.length() > 80) result = result.substring(result.length() - 80);
    return result.isBlank() ? "file" : result;
  }
}
