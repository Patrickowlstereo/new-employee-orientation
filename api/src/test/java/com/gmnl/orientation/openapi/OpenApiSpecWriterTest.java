package com.gmnl.orientation.openapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 将 springdoc 生成的 OpenAPI 规范导出为 {@code packages/shared/openapi.json},作为前后端契约制品。
 *
 * <p>仅显式触发:{@code mvn -f api/pom.xml test -Dtest=OpenApiSpecWriterTest -Ddump.openapi=true}。
 * 默认 {@code mvn test}(42 项)不启用本类——系统属性未设时整类被 JUnit 禁用,不加载 Spring 上下文,
 * 故不影响常规测试速度与结果。
 *
 * <p>用 MockMvc 取 {@code /v3/api-docs} 的 JSON 并美化写出,保证 git diff 稳定;
 * 后端改 DTO 后重跑即可,前端再 {@code pnpm gen:api} 重新派生类型。
 */
@SpringBootTest
@AutoConfigureMockMvc
// 生产配置默认关闭 springdoc 且 JWT 密钥无默认值;契约导出必须可用,
// 故在测试上下文显式开启 api-docs 并注入仅测试用的密钥。
@TestPropertySource(properties = {
    "springdoc.api-docs.enabled=true",
    "app.jwt.secret=test-secret-test-secret-test-secret-test-secret-32b"
})
@EnabledIfSystemProperty(named = "dump.openapi", matches = "true")
class OpenApiSpecWriterTest {

  @Autowired MockMvc mockMvc;

  @Test
  void dumpOpenApiJson() throws Exception {
    String compact = mockMvc.perform(get("/v3/api-docs").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

    // 美化输出,便于 review 与稳定 diff
    ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    Object parsed = mapper.readValue(compact, Object.class);
    String pretty = mapper.writeValueAsString(parsed);

    Path out = resolveRepoRoot().resolve("packages/shared/openapi.json");
    Files.createDirectories(out.getParent());
    Files.writeString(out, pretty + System.lineSeparator(), StandardCharsets.UTF_8);

    assertThat(pretty).contains("\"openapi\"").contains("\"paths\"").contains("\"components\"");
    System.out.println("OpenAPI 规范已写出: " + out.toAbsolutePath());
  }

  /** 从测试 JVM 工作目录(模块 api/)向上找到同时含 api/ 与 packages/ 的仓库根。 */
  private static Path resolveRepoRoot() {
    Path dir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
    while (dir != null && Files.isDirectory(dir)) {
      if (Files.isDirectory(dir.resolve("api")) && Files.isDirectory(dir.resolve("packages"))) {
        return dir;
      }
      dir = dir.getParent();
    }
    throw new IllegalStateException(
        "找不到仓库根(含 api/ 与 packages/): " + Paths.get(System.getProperty("user.dir")));
  }
}
