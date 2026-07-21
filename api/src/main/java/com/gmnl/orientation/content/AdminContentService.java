package com.gmnl.orientation.content;

import com.gmnl.orientation.progress.ProgressRepository;
import com.gmnl.orientation.user.User;
import com.gmnl.orientation.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
 * 后台内容管理：文档文件的上传/替换/删除（阶段 4）+ 机构/小岛/文档行的 CRUD（阶段 5）。
 */
@Service
public class AdminContentService {

  private static final Logger log = LoggerFactory.getLogger(AdminContentService.class);

  private final DocRepository docRepo;
  private final InstitutionRepository institutionRepo;
  private final IslandRepository islandRepo;
  private final UserRepository userRepo;
  private final ProgressRepository progressRepo;
  private final DocFileStorage storage;

  public AdminContentService(DocRepository docRepo, InstitutionRepository institutionRepo,
                             IslandRepository islandRepo, UserRepository userRepo,
                             ProgressRepository progressRepo, DocFileStorage storage) {
    this.docRepo = docRepo;
    this.institutionRepo = institutionRepo;
    this.islandRepo = islandRepo;
    this.userRepo = userRepo;
    this.progressRepo = progressRepo;
    this.storage = storage;
  }

  // ========== 文档文件（阶段 4）==========

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
    Doc doc = docRepo.findById(docId).orElseThrow(() -> notFound("文档不存在"));

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
    log.info("审计: {} 上传/替换文件 文档#{}《{}》 类型={} 旧路径={}", operator(), doc.getId(), doc.getTitle(), ext, oldPath);

    // 旧文件延迟到事务提交后再删:回滚时 DB 仍指向旧文件,文件必须还在
    deleteFileAfterCommit(oldPath);

    String uploaderName = userRepo.findById(uploaderId).map(User::getName).orElse(null);
    return AdminDocDto.from(doc, uploaderName);
  }

  /** 删除文档文件（幂等）：清空路径/类型/审计，物理文件在事务提交后删除。保留文档行。 */
  @Transactional
  public AdminDocDto deleteFile(Long docId) {
    Doc doc = docRepo.findById(docId).orElseThrow(() -> notFound("文档不存在"));
    String oldPath = doc.getFilePath();
    doc.setFilePath(null);
    doc.setFileType(null);
    doc.setFileUploadedAt(null);
    doc.setFileUploadedBy(null);
    docRepo.save(doc);
    deleteFileAfterCommit(oldPath);
    log.info("审计: {} 删除文件 文档#{}《{}》 路径={}", operator(), doc.getId(), doc.getTitle(), oldPath);
    return AdminDocDto.from(doc, null);
  }

  // ========== 机构 CRUD（阶段 5）==========

  @Transactional
  public InstitutionDto createInstitution(InstitutionUpsertRequest req) {
    Institution inst = new Institution();
    inst.setKey(req.key());
    inst.setName(req.name());
    inst.setOrder(req.order() != null ? req.order() : 0);
    institutionRepo.save(inst);
    log.info("审计: {} 新增机构 #{}《{}》", operator(), inst.getId(), inst.getName());
    return new InstitutionDto(inst.getId(), inst.getKey(), inst.getName(), inst.getOrder(), List.of());
  }

  @Transactional
  public InstitutionDto updateInstitution(Long id, InstitutionUpsertRequest req) {
    Institution inst = institutionRepo.findById(id).orElseThrow(() -> notFound("机构不存在"));
    inst.setKey(req.key());
    inst.setName(req.name());
    if (req.order() != null) inst.setOrder(req.order());
    institutionRepo.save(inst);
    log.info("审计: {} 修改机构 #{}《{}》", operator(), inst.getId(), inst.getName());
    return new InstitutionDto(inst.getId(), inst.getKey(), inst.getName(), inst.getOrder(), List.of());
  }

  @Transactional
  public void deleteInstitution(Long id) {
    if (!institutionRepo.existsById(id)) throw notFound("机构不存在");
    if (islandRepo.countByInstitutionId(id) > 0) {
      throw new IllegalArgumentException("该机构下还有小岛，请先删除小岛");
    }
    institutionRepo.deleteById(id);
    log.info("审计: {} 删除机构 #{}", operator(), id);
  }

  // ========== 小岛 CRUD ==========

  @Transactional
  public IslandDto createIsland(IslandUpsertRequest req) {
    Island isl = newIsland(req);
    islandRepo.save(isl);
    log.info("审计: {} 新增小岛 #{}《{}》", operator(), isl.getId(), isl.getName());
    return IslandDto.from(isl);
  }

  @Transactional
  public IslandDto updateIsland(Long id, IslandUpsertRequest req) {
    Island isl = islandRepo.findById(id).orElseThrow(() -> notFound("小岛不存在"));
    applyIslandFields(isl, req);
    islandRepo.save(isl);
    log.info("审计: {} 修改小岛 #{}《{}》", operator(), isl.getId(), isl.getName());
    return IslandDto.from(isl);
  }

  @Transactional
  public void deleteIsland(Long id) {
    if (!islandRepo.existsById(id)) throw notFound("小岛不存在");
    if (docRepo.countByIslandId(id) > 0) {
      throw new IllegalArgumentException("该小岛下还有文档，请先处理文档");
    }
    islandRepo.deleteById(id);
    log.info("审计: {} 删除小岛 #{}", operator(), id);
  }

  // ========== 文档行 CRUD ==========

  @Transactional
  public AdminDocDto createDoc(DocUpsertRequest req) {
    Island isl = islandRepo.findById(req.islandId())
        .orElseThrow(() -> new IllegalArgumentException("所属小岛不存在"));
    if (!isl.getInstitutionId().equals(req.institutionId())) {
      throw new IllegalArgumentException("文档的机构与小岛所属机构不一致");
    }
    Doc doc = new Doc();
    applyDocFields(doc, req, true);
    doc.setInstitutionId(req.institutionId());
    doc.setIslandId(req.islandId());
    docRepo.save(doc);
    log.info("审计: {} 新增文档 #{}《{}》", operator(), doc.getId(), doc.getTitle());
    return AdminDocDto.from(doc, null);
  }

  @Transactional
  public AdminDocDto updateDoc(Long id, DocUpsertRequest req) {
    Doc doc = docRepo.findById(id).orElseThrow(() -> notFound("文档不存在"));
    Island isl = islandRepo.findById(req.islandId())
        .orElseThrow(() -> new IllegalArgumentException("所属小岛不存在"));
    if (!isl.getInstitutionId().equals(req.institutionId())) {
      throw new IllegalArgumentException("文档的机构与小岛所属机构不一致");
    }
    applyDocFields(doc, req, false);
    doc.setInstitutionId(req.institutionId());
    doc.setIslandId(req.islandId());
    docRepo.save(doc);
    log.info("审计: {} 修改文档 #{}《{}》", operator(), doc.getId(), doc.getTitle());
    String name = doc.getFileUploadedBy() == null ? null
        : userRepo.findById(doc.getFileUploadedBy()).map(User::getName).orElse(null);
    return AdminDocDto.from(doc, name);
  }

  /** 硬删文档行：有学习记录则拒绝（建议停用），否则删行，物理文件在事务提交后删除。 */
  @Transactional
  public void deleteDocRow(Long id) {
    Doc doc = docRepo.findById(id).orElseThrow(() -> notFound("文档不存在"));
    if (progressRepo.countByDocId(id) > 0) {
      throw new IllegalArgumentException("该文档已有学习记录，请改用停用");
    }
    String oldPath = doc.getFilePath();
    docRepo.delete(doc);
    deleteFileAfterCommit(oldPath);
    log.info("审计: {} 删除文档行 #{}《{}》 文件路径={}", operator(), doc.getId(), doc.getTitle(), oldPath);
  }

  /**
   * 把物理文件删除注册到当前事务的 afterCommit:提交前（含回滚）文件保留，
   * 避免 DB 指向已删除文件导致永久 404。无活动事务时（如单元测试直接调用）立即删除。
   * 删除失败只记 WARN,不阻断已提交的业务结果,留待运维清理。
   */
  private void deleteFileAfterCommit(String path) {
    if (path == null || path.isBlank()) return;
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          deletePhysicalQuietly(path);
        }
      });
    } else {
      deletePhysicalQuietly(path);
    }
  }

  private void deletePhysicalQuietly(String path) {
    try {
      storage.delete(path);
    } catch (Exception e) {
      log.warn("物理文件删除失败（留待运维清理）: {}", path, e);
    }
  }

  // ========== 内部辅助 ==========

  private Island newIsland(IslandUpsertRequest req) {
    if (!institutionRepo.existsById(req.institutionId())) {
      throw new IllegalArgumentException("所属机构不存在");
    }
    Island isl = new Island();
    isl.setInstitutionId(req.institutionId());
    applyIslandFields(isl, req);
    return isl;
  }

  private void applyIslandFields(Island isl, IslandUpsertRequest req) {
    isl.setKey(req.key());
    isl.setName(req.name());
    if (req.order() != null) isl.setOrder(req.order());
    if (isl.getInstitutionId() == null) isl.setInstitutionId(req.institutionId());
    else if (!isl.getInstitutionId().equals(req.institutionId())) {
      // 迁移到别的机构时，目标机构须存在
      if (!institutionRepo.existsById(req.institutionId())) {
        throw new IllegalArgumentException("所属机构不存在");
      }
      isl.setInstitutionId(req.institutionId());
    }
  }

  private void applyDocFields(Doc doc, DocUpsertRequest req, boolean isCreate) {
    doc.setTitle(req.title());
    doc.setCategory(req.category());
    doc.setRequired(Boolean.TRUE.equals(req.required()));
    if (isCreate) {
      doc.setOrder(req.order() != null ? req.order() : 0);
      doc.setActive(req.active() == null || req.active());
    } else {
      if (req.order() != null) doc.setOrder(req.order());
      if (req.active() != null) doc.setActive(req.active());
    }
  }

  private ResponseStatusException notFound(String msg) {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
  }

  /** 审计日志的操作人:SecurityContext 中存的是 JWT subject(userId),尽力解析为 username。 */
  private String operator() {
    var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getPrincipal() == null) return "unknown";
    String userId = String.valueOf(auth.getPrincipal());
    try {
      return userRepo.findById(Long.parseLong(userId)).map(User::getUsername).orElse("user#" + userId);
    } catch (NumberFormatException e) {
      return userId;
    }
  }
}
