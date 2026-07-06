package com.gmnl.orientation.content;

import com.gmnl.orientation.common.ApiError;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ContentController {

  private final ContentService contentService;
  private final String uploadsDir;

  public ContentController(ContentService contentService,
                           @Value("${app.uploads.dir:./uploads}") String uploadsDir) {
    this.contentService = contentService;
    this.uploadsDir = uploadsDir;
  }

  @GetMapping("/institutions")
  public List<InstitutionDto> institutions() {
    return contentService.listInstitutions();
  }

  @GetMapping("/islands")
  public List<IslandDto> islands(@RequestParam(required = false) Long institutionId) {
    return contentService.listIslands(institutionId);
  }

  @GetMapping("/docs")
  public List<DocDto> docs(@RequestParam(required = false) Long islandId,
                           @RequestParam(required = false) Boolean required) {
    return contentService.listDocs(islandId, required);
  }

  @GetMapping("/docs/{id}")
  public DocDto doc(@PathVariable Long id) {
    return DocDto.from(contentService.getDocEntity(id));
  }

  @GetMapping("/docs/{id}/file")
  public ResponseEntity<?> downloadFile(@PathVariable Long id) {
    Doc doc = contentService.getDocEntity(id);
    if (doc.getFilePath() == null || doc.getFilePath().isBlank()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(new ApiError("NO_FILE", "该文档尚未上传文件"));
    }
    File file = new File(uploadsDir, doc.getFilePath());
    if (!file.exists()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(new ApiError("NO_FILE", "文件不存在"));
    }
    String filename = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8).replace("+", "%20");
    // 按扩展名回真实 MIME，供学习端 blob 预览（图片/视频/音频/PDF）正确识别；下载行为不变。
    MediaType contentType = FileTypeSupport.mediaTypeOf(doc.getFileType());
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
        .contentType(contentType)
        .contentLength(file.length())
        .body(new FileSystemResource(file));
  }
}
