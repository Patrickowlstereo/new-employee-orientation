package com.gmnl.orientation.config;

import com.gmnl.orientation.security.JwtService;
import com.gmnl.orientation.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtService jwtService;

  public JwtAuthFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String token = extractToken(req);
    if (token != null) {
      try {
        Claims c = jwtService.parse(token);
        String role = c.get("role", String.class);
        var auth = new UsernamePasswordAuthenticationToken(
            c.getSubject(), null,
            List.of(new SimpleGrantedAuthority("ROLE_" + UserRole.valueOf(role).name())));
        SecurityContextHolder.getContext().setAuthentication(auth);
      } catch (JwtException | IllegalArgumentException e) {
        SecurityContextHolder.clearContext();
      }
    }
    chain.doFilter(req, res);
  }

  /**
   * 取 JWT:优先 Authorization: Bearer;仅文件下载接口(/api/docs/&lt;id&gt;/file)允许 ?token= 查询参数兜底。
   *
   * <p>原因:&lt;video&gt;/&lt;img&gt;/&lt;iframe&gt; 无法附加 Authorization 头,流式预览需直链。
   * token 入 URL 有日志/历史暴露风险,故仅限文件接口,范围最小化。内部工具可接受。
   */
  private String extractToken(HttpServletRequest req) {
    String header = req.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      return header.substring(7);
    }
    String uri = req.getRequestURI();
    if (uri != null && uri.startsWith("/api/docs/") && uri.endsWith("/file")) {
      return req.getParameter("token");
    }
    return null;
  }
}
