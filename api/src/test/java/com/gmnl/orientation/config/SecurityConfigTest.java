package com.gmnl.orientation.config;

import com.gmnl.orientation.content.AdminContentController;
import com.gmnl.orientation.content.AdminContentService;
import com.gmnl.orientation.content.AdminInstitutionController;
import com.gmnl.orientation.content.AdminIslandController;
import com.gmnl.orientation.content.ContentController;
import com.gmnl.orientation.content.ContentService;
import com.gmnl.orientation.content.DocFileStorage;
import com.gmnl.orientation.progress.ProgressController;
import com.gmnl.orientation.progress.ProgressService;
import com.gmnl.orientation.stats.AdminStatsController;
import com.gmnl.orientation.stats.StatsService;
import com.gmnl.orientation.user.AuthController;
import com.gmnl.orientation.user.AuthService;
import com.gmnl.orientation.user.CurrentUserResolver;
import com.gmnl.orientation.user.UserRepository;
import com.gmnl.orientation.security.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, ProgressController.class,
    AdminContentController.class, AdminInstitutionController.class, AdminIslandController.class,
    AdminStatsController.class, ContentController.class})
@Import({SecurityConfig.class, JwtAuthFilter.class})
class SecurityConfigTest {

  @Autowired MockMvc mockMvc;
  @MockBean AuthService authService;
  @MockBean UserRepository userRepository;
  @MockBean CurrentUserResolver currentUserResolver;
  @MockBean ProgressService progressService;
  @MockBean AdminContentService adminContentService;
  @MockBean StatsService statsService;
  @MockBean ContentService contentService;
  @MockBean DocFileStorage storage;
  @MockBean JwtService jwtService;

  @Test
  void unauthenticatedRequestToSecuredEndpointReturns401() throws Exception {
    mockMvc.perform(get("/api/progress")).andExpect(status().isUnauthorized());
  }

  @Test
  void adminDocsListRequiresAuth() throws Exception {
    mockMvc.perform(get("/api/admin/docs")).andExpect(status().isUnauthorized());
  }

  @Test
  void adminDocUploadRequiresAuth() throws Exception {
    mockMvc.perform(multipart("/api/admin/docs/1/upload").file("file", new byte[]{1}))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void adminInstitutionMutationRequiresAuth() throws Exception {
    mockMvc.perform(post("/api/admin/institutions")
        .contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void adminIslandMutationRequiresAuth() throws Exception {
    mockMvc.perform(post("/api/admin/islands")
        .contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void adminStatsRequiresAuth() throws Exception {
    mockMvc.perform(get("/api/admin/stats/overview")).andExpect(status().isUnauthorized());
  }

  @Test
  void fileEndpointWithoutTokenReturns401() throws Exception {
    mockMvc.perform(get("/api/docs/1/file")).andExpect(status().isUnauthorized());
  }

  @Test
  void fileEndpointAcceptsQueryToken() throws Exception {
    // 模拟合法 token:parse 返回带 subject/role 的 Claims
    Claims claims = org.mockito.Mockito.mock(Claims.class);
    when(claims.getSubject()).thenReturn("1");
    when(claims.get("role", String.class)).thenReturn("USER");
    when(jwtService.parse("tok")).thenReturn(claims);
    // 鉴权通过后控制器抛 404(非 401)即证明 query token 生效
    when(contentService.getDocEntity(1L))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "文件不存在"));

    mockMvc.perform(get("/api/docs/1/file").param("token", "tok"))
        .andExpect(status().isNotFound());
  }
}
