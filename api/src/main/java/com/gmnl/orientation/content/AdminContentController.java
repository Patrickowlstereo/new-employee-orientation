package com.gmnl.orientation.content;

import com.gmnl.orientation.user.CurrentUserResolver;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 后台内容管理接口（仅 ADMIN）。
 * 路径前缀 /api/admin/docs 已由 SecurityConfig 限定 hasRole("ADMIN")。
 */
@RestController
@RequestMapping("/api/admin/docs")
public class AdminContentController {

  private final AdminContentService service;
  private final CurrentUserResolver currentUser;

  public AdminContentController(AdminContentService service, CurrentUserResolver currentUser) {
    this.service = service;
    this.currentUser = currentUser;
  }

  /** 全量文档列表（含未激活与上传审计）。 */
  @GetMapping
  public List<AdminDocDto> list() {
    return service.listDocs();
  }

  /** 新建文档行（文件另走 /upload）。 */
  @PostMapping
  public AdminDocDto create(@Valid @RequestBody DocUpsertRequest req) {
    return service.createDoc(req);
  }

  /** 更新文档行（标题/分类/小岛/必修/序号/启用）。 */
  @PutMapping("/{id}")
  public AdminDocDto update(@PathVariable Long id, @Valid @RequestBody DocUpsertRequest req) {
    return service.updateDoc(id, req);
  }

  /** 删除文档行（有学习记录则拒绝，建议改用停用）。 */
  @DeleteMapping("/{id}")
  public void deleteRow(@PathVariable Long id) {
    service.deleteDocRow(id);
  }

  /** 上传/替换某文档的文件（multipart: file=...）。 */
  @PostMapping("/{id}/upload")
  public AdminDocDto upload(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
    return service.uploadFile(id, file, currentUser.userId());
  }

  /** 删除某文档的文件（保留文档行，仅清空文件）。 */
  @DeleteMapping("/{id}/file")
  public AdminDocDto deleteFile(@PathVariable Long id) {
    return service.deleteFile(id);
  }
}
