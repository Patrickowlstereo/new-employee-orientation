package com.gmnl.orientation.content;

import com.gmnl.orientation.user.CurrentUserResolver;
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
