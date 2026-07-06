package com.gmnl.orientation.config;

import com.gmnl.orientation.content.AdminContentController;
import com.gmnl.orientation.content.AdminContentService;
import com.gmnl.orientation.content.AdminInstitutionController;
import com.gmnl.orientation.content.AdminIslandController;
import com.gmnl.orientation.progress.ProgressController;
import com.gmnl.orientation.progress.ProgressService;
import com.gmnl.orientation.stats.AdminStatsController;
import com.gmnl.orientation.stats.StatsService;
import com.gmnl.orientation.user.AuthController;
import com.gmnl.orientation.user.AuthService;
import com.gmnl.orientation.user.CurrentUserResolver;
import com.gmnl.orientation.user.UserRepository;
import com.gmnl.orientation.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, ProgressController.class,
    AdminContentController.class, AdminInstitutionController.class, AdminIslandController.class,
    AdminStatsController.class})
@Import({SecurityConfig.class, JwtAuthFilter.class})
class SecurityConfigTest {

  @Autowired MockMvc mockMvc;
  @MockBean AuthService authService;
  @MockBean UserRepository userRepository;
  @MockBean CurrentUserResolver currentUserResolver;
  @MockBean ProgressService progressService;
  @MockBean AdminContentService adminContentService;
  @MockBean StatsService statsService;
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
}
