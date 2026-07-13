package com.gmnl.orientation.content;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * 后台管理用文档视图：在 learner 的 DocDto 基础上补齐文件大类与上传审计，
 * 不暴露 filePath（防止泄露存储路径）。
 */
public record AdminDocDto(Long id, String title, @Schema(nullable = true) String category, Long institutionId, Long islandId,
                          Boolean required, @Schema(nullable = true) String fileType, FileTypeSupport.Category fileCategory, Integer order, Boolean active,
                          @Schema(nullable = true) Instant uploadedAt, @Schema(nullable = true) String uploadedByName) {

  public static AdminDocDto from(Doc d, String uploadedByName) {
    // fileCategory 用枚举类型(而非 String),让 OpenAPI 规范识别为枚举,
    // 前端 FileCategory 类型可由规范派生;Jackson 仍按 name() 序列化,JSON 不变。
    FileTypeSupport.Category fileCategory = (d.getFileType() != null)
        ? FileTypeSupport.categoryOf(d.getFileType())
        : FileTypeSupport.Category.OTHER;
    return new AdminDocDto(d.getId(), d.getTitle(), d.getCategory(), d.getInstitutionId(), d.getIslandId(),
        d.getRequired(), d.getFileType(), fileCategory, d.getOrder(), d.getActive(),
        d.getFileUploadedAt(), uploadedByName);
  }
}
