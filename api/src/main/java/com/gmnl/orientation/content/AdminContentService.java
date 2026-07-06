package com.gmnl.orientation.content;

import com.gmnl.orientation.user.User;
import com.gmnl.orientation.user.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 后台内容管理：文档文件的上传/替换/删除与全量列表。
 * 仅负责"已有文档行"的文件操作；文档行本身的增删改属阶段 5。
 */
@Service
public class AdminContentService {

  private final DocRepository docRepo;
  private final UserRepository userRepo;
  private final DocFileStorage storage;

  public AdminContentService(DocRepository docRepo, UserRepository userRepo, DocFileStorage storage) {
    this.docRepo = docRepo;
    this.userRepo = userRepo;
    this.storage = storage;
  }

  /** 全量文档（含 inactive），按机构/小岛/序号排序，附带上传人姓名。 */
  public List<AdminDocDto> listDocs() {
    List<Doc> docs = docRepo.findAll(Sort.by("institutionId", "islandId", "order"));
    Set<Long> uploaderIds = docs.stream()
        .map(Doc::getFileUploadedBy)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    Map<Long, String> names = uploaderIds.isEmpty() ? Map.of()
        : userRepo.findAllById(uploaderIds).stream()
            .collect(Collectors.toMap(User::getId, User::getName));
    return docs.stream()
        .map(d -> AdminDocDto.from(d, d.getFileUploadedBy() == null ? null : names.get(d.getFileUploadedBy())))
        .toList();
  }

  /** 上传/替换文档文件：校验类型 → 落盘 → 写回路径/类型/审计 → 清理旧文件。 */
  @Transactional
  public AdminDocDto uploadFile(Long docId, MultipartFile file, Long uploaderId) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("文件为空");
    }
    String ext = FileTypeSupport.extensionOf(file.getOriginalFilename());
    if (!FileTypeSupport.isAllowed(ext)) {
      throw new IllegalArgumentException("不支持的文件类型");
    }
    Doc doc = docRepo.findById(docId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "文档不存在"));

    String oldPath = doc.getFilePath();
    String sanitized = FileTypeSupport.sanitizeFilename(file.getOriginalFilename());
    String relative;
    try {
      relative = storage.store(docId, sanitized, file.getInputStream());
    } catch (IOException e) {
      throw new IllegalStateException("文件保存失败", e);
    }

    doc.setFilePath(relative);
    doc.setFileType(ext);
    doc.setFileUploadedAt(Instant.now());
    doc.setFileUploadedBy(uploaderId);
    docRepo.save(doc);

    // 新文件落盘成功后再清理旧文件，避免替换失败时丢文件
    if (oldPath != null && !oldPath.isBlank()) {
      storage.delete(oldPath);
    }

    String uploaderName = userRepo.findById(uploaderId).map(User::getName).orElse(null);
    return AdminDocDto.from(doc, uploaderName);
  }

  /** 删除文档文件（幂等）：清空路径/类型/审计，并删除物理文件。 */
  @Transactional
  public AdminDocDto deleteFile(Long docId) {
    Doc doc = docRepo.findById(docId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "文档不存在"));
    if (doc.getFilePath() != null && !doc.getFilePath().isBlank()) {
      storage.delete(doc.getFilePath());
    }
    doc.setFilePath(null);
    doc.setFileType(null);
    doc.setFileUploadedAt(null);
    doc.setFileUploadedBy(null);
    docRepo.save(doc);
    return AdminDocDto.from(doc, null);
  }
}
