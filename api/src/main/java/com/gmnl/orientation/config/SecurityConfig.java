package com.gmnl.orientation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

  private final JwtAuthFilter jwtAuthFilter;

  public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
    this.jwtAuthFilter = jwtAuthFilter;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // 允许同源 iframe 嵌入(企业文化转盘等 html 模块由 DocPage 用沙箱 iframe 加载);
        // 仍挡跨源 clickjacking。默认 DENY 会导致浏览器在 iframe 里显示"拒绝连接"。
        .headers(h -> h.frameOptions(fo -> fo.sameOrigin()))
        .authorizeHttpRequests(auth -> auth
            // OpenAPI 规范与 Swagger UI:内部开发用,无鉴权开放(anyRequest().permitAll() 本就放行,
            // 显式声明以便后续若收紧规则时不被遗漏)。JwtAuthFilter 无 token 时 no-op,无需特殊处理。
            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                "/swagger-resources/**", "/webjars/**").permitAll()
            .requestMatchers("/api/auth/login").permitAll()
            // 存活探针放行健康检查,其余 actuator 路径一律拒绝
            //(management.endpoints.web.exposure.include=health 已使其不可达,这里再上一道保险)
            .requestMatchers("/actuator/health").permitAll()
            .requestMatchers("/actuator/**").denyAll()
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .requestMatchers("/api/**").authenticated()
            .anyRequest().permitAll())
        .exceptionHandling(e -> e
            .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
