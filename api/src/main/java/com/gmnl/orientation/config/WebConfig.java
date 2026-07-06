package com.gmnl.orientation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Value("${app.static.web-dir:./dist/web}")
  private String webDir;
  @Value("${app.static.admin-dir:./dist/admin}")
  private String adminDir;

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
        .allowedOrigins("http://localhost:5173", "http://localhost:5174")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true);
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // admin 后台：/admin/** → admin dist（base=/admin/ 构建产物）
    registry.addResourceHandler("/admin/**")
        .addResourceLocations("file:" + adminDir + "/")
        .resourceChain(true);
    // web 游戏：根路径 → web dist
    registry.addResourceHandler("/**")
        .addResourceLocations("file:" + webDir + "/")
        .resourceChain(true);
  }
}
