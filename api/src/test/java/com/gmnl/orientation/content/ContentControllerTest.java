package com.gmnl.orientation.content;

import com.gmnl.orientation.common.GlobalExceptionHandler;
import com.gmnl.orientation.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证文件接口的 Range 流式逻辑(鉴权关闭,聚焦控制器)。
 */
@WebMvcTest(controllers = ContentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ContentControllerTest {

  @Autowired MockMvc mockMvc;
  @MockBean ContentService contentService;
  @MockBean DocFileStorage storage;
  @MockBean JwtService jwtService; // 供被安全自动配置拉起的 JwtAuthFilter 注入(addFilters=false 已禁用过滤)
  @TempDir Path tempDir;

  private Doc docWith(String relPath, String fileType) {
    Doc d = new Doc();
    d.setId(1L);
    d.setFilePath(relPath);
    d.setFileType(fileType);
    return d;
  }

  @Test
  void fileWithoutRangeReturns200AndAcceptRanges() throws Exception {
    Path f = tempDir.resolve("a.mp4");
    Files.write(f, new byte[1000]);
    when(contentService.getDocEntity(1L)).thenReturn(docWith("docs/1/a.mp4", "mp4"));
    when(storage.resolve("docs/1/a.mp4")).thenReturn(f);

    mockMvc.perform(get("/api/docs/1/file"))
        .andExpect(status().isOk())
        .andExpect(header().string("Accept-Ranges", "bytes"))
        .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("inline")));
  }

  @Test
  void fileWithRangeReturns206AndContentRange() throws Exception {
    Path f = tempDir.resolve("a.mp4");
    Files.write(f, new byte[1000]);
    when(contentService.getDocEntity(1L)).thenReturn(docWith("docs/1/a.mp4", "mp4"));
    when(storage.resolve("docs/1/a.mp4")).thenReturn(f);

    mockMvc.perform(get("/api/docs/1/file").header("Range", "bytes=0-99"))
        .andExpect(status().isPartialContent())
        .andExpect(header().string("Content-Range", "bytes 0-99/1000"))
        .andExpect(header().string("Accept-Ranges", "bytes"));
  }

  @Test
  void openEndedRangeClampsToEndOfFile() throws Exception {
    Path f = tempDir.resolve("a.mp4");
    Files.write(f, new byte[500]);
    when(contentService.getDocEntity(1L)).thenReturn(docWith("docs/1/a.mp4", "mp4"));
    when(storage.resolve("docs/1/a.mp4")).thenReturn(f);

    // bytes=400- 越界应裁剪到 400-499
    mockMvc.perform(get("/api/docs/1/file").header("Range", "bytes=400-"))
        .andExpect(status().isPartialContent())
        .andExpect(header().string("Content-Range", "bytes 400-499/500"));
  }

  @Test
  void missingFileOnDiskReturns404() throws Exception {
    when(contentService.getDocEntity(1L)).thenReturn(docWith("docs/1/a.mp4", "mp4"));
    when(storage.resolve("docs/1/a.mp4")).thenReturn(tempDir.resolve("nope.mp4"));

    mockMvc.perform(get("/api/docs/1/file")).andExpect(status().isNotFound());
  }

  @Test
  void nullFilePathReturns404() throws Exception {
    when(contentService.getDocEntity(1L)).thenReturn(docWith(null, "mp4"));

    mockMvc.perform(get("/api/docs/1/file")).andExpect(status().isNotFound());
  }
}
