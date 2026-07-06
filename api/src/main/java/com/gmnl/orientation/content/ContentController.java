package com.gmnl.orientation.content;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ContentController {

  private final ContentService contentService;
  private final DocFileStorage storage;

  public ContentController(ContentService contentService, DocFileStorage storage) {
    this.contentService = contentService;
    this.storage = storage;
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

  /**
   * 文件下载/流式预览。支持 HTTP Range(206 Partial Content),供 &lt;video&gt; 等媒体元素拖动进度条、分片加载。
   *
   * <p>Disposition 用 inline(内联预览);学习端的下载按钮另走 blob + a.download,不受影响。
   * 路径经 {@link DocFileStorage#resolve} 做穿越防护。
   */
  @GetMapping("/docs/{id}/file")
  public ResponseEntity<?> downloadFile(@PathVariable Long id,
                                        @RequestHeader(value = "Range", required = false) String rangeHeader)
      throws IOException {
    Doc doc = contentService.getDocEntity(id);
    if (doc.getFilePath() == null || doc.getFilePath().isBlank()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "该文档尚未上传文件");
    }
    File file = storage.resolve(doc.getFilePath()).toFile();
    if (!file.exists()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文件不存在");
    }
    String filename = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8).replace("+", "%20");
    MediaType contentType = FileTypeSupport.mediaTypeOf(doc.getFileType());
    String disposition = "inline; filename*=UTF-8''" + filename;
    long total = file.length();

    // 有 Range → 206,手动返回字节区段(显式 Content-Range/Content-Length,走默认 ResourceHttpMessageConverter)
    if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
      List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
      if (!ranges.isEmpty()) {
        HttpRange range = ranges.get(0);
        long start = range.getRangeStart(total);
        long end = range.getRangeEnd(total);
        long length = end - start + 1;
        InputStream region = bounded(Files.newInputStream(file.toPath()), start, length);
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
            .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + total)
            .contentType(contentType)
            .contentLength(length)
            .body(new InputStreamResource(region));
      }
    }

    // 无 Range → 200 全量,声明支持断点续传
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
        .contentType(contentType)
        .contentLength(total)
        .body(new FileSystemResource(file));
  }

  /**
   * 从输入流跳过 {@code skip} 字节后,只读取 {@code limit} 字节的包装流,供 Range 区段输出。
   * 跳过采用循环以避免 skip(…) 单次未跳满。流由调用方负责关闭(此处交给 InputStreamResource/转换器)。
   */
  private static InputStream bounded(InputStream in, long skip, long limit) throws IOException {
    long toSkip = skip;
    while (toSkip > 0) {
      long s = in.skip(toSkip);
      if (s <= 0) break;
      toSkip -= s;
    }
    return new InputStream() {
      long remaining = limit;

      @Override
      public int read() throws IOException {
        if (remaining <= 0) return -1;
        int b = in.read();
        if (b != -1) remaining--;
        return b;
      }

      @Override
      public int read(byte[] b, int off, int len) throws IOException {
        if (remaining <= 0) return -1;
        int n = in.read(b, off, (int) Math.min(len, remaining));
        if (n > 0) remaining -= n;
        return n;
      }

      @Override
      public void close() throws IOException {
        in.close();
      }
    };
  }
}
