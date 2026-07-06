package com.gmnl.orientation.content;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 学习文件本地存储：统一负责写入/删除/解析，并做路径穿越防护。
 *
 * <p>文件存放于 {@code app.uploads.dir}（默认 ./uploads）下 {@code docs/{docId}/{毫秒}_{净化名}}，
 * 数据库仅存相对路径。该目录不在静态资源映射内，只能经鉴权下载接口访问。
 */
@Component
public class DocFileStorage {

  private final Path root;

  public DocFileStorage(@Value("${app.uploads.dir:./uploads}") String dir) {
    this.root = Paths.get(dir).toAbsolutePath().normalize();
  }

  @PostConstruct
  void init() throws IOException {
    Files.createDirectories(root);
  }

  /** 存储输入流为文件，返回相对 root 的路径（/ 分隔）。 */
  public String store(Long docId, String sanitizedFilename, InputStream in) throws IOException {
    String relative = "docs/" + docId + "/" + System.currentTimeMillis() + "_" + sanitizedFilename;
    Path target = resolve(relative);
    Files.createDirectories(target.getParent());
    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
    return relative;
  }

  /** 删除相对路径对应文件；不存在或路径非法则静默（best-effort）。 */
  public void delete(String relativePath) {
    if (relativePath == null || relativePath.isBlank()) return;
    try {
      Files.deleteIfExists(resolve(relativePath));
    } catch (IOException ignored) {
      // 清理失败不阻断业务，留待后续运维清理
    }
  }

  /** 解析相对路径并做路径穿越防护：规范化后必须仍在 root 之内。 */
  public Path resolve(String relativePath) {
    Path resolved = root.resolve(relativePath).normalize();
    if (!resolved.startsWith(root)) {
      throw new IllegalArgumentException("非法文件路径");
    }
    return resolved;
  }

  public boolean exists(String relativePath) {
    if (relativePath == null || relativePath.isBlank()) return false;
    return Files.exists(resolve(relativePath));
  }
}
