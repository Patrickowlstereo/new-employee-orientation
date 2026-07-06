package com.gmnl.orientation.content;

import java.time.Instant;

/**
 * 后台管理用文档视图：在 learner 的 DocDto 基础上补齐文件大类与上传审计，
 * 不暴露 filePath（防止泄露存储路径）。
 */
public record AdminDocDto(Long id, String title, String category, Long institutionId, Long islandId,
                          Boolean required, String fileType, String fileCategory, Integer order, Boolean active,
                          Instant uploadedAt, String uploadedByName) {

  public static AdminDocDto from(Doc d, String uploadedByName) {
    String fileCategory = (d.getFileType() != null)
        ? FileTypeSupport.categoryOf(d.getFileType()).name()
        : FileTypeSupport.Category.OTHER.name();
    return new AdminDocDto(d.getId(), d.getTitle(), d.getCategory(), d.getInstitutionId(), d.getIslandId(),
        d.getRequired(), d.getFileType(), fileCategory, d.getOrder(), d.getActive(),
        d.getFileUploadedAt(), uploadedByName);
  }
}
