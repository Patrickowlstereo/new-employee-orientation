package com.gmnl.orientation.content;

public record DocDto(Long id, String title, String category, Long institutionId, Long islandId,
                     Boolean required, String fileType, Integer order, Boolean active,
                     String linkUrl) {
  public static DocDto from(Doc d) {
    // html 类互动模块：filePath 指向学习端 public 静态资源，暴露为 linkUrl 供沙箱 iframe 预览；
    // 普通上传文件不暴露内部存储路径（仍经鉴权下载接口访问）。
    String linkUrl = ("html".equals(d.getFileType()) && d.getFilePath() != null && !d.getFilePath().isBlank())
        ? "/" + d.getFilePath() : null;
    return new DocDto(d.getId(), d.getTitle(), d.getCategory(), d.getInstitutionId(),
        d.getIslandId(), d.getRequired(), d.getFileType(), d.getOrder(), d.getActive(), linkUrl);
  }
}
