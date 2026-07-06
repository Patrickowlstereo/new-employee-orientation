package com.gmnl.orientation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA 前端路由刷新兜底：未匹配静态资源的浏览器路由请求 forward 回对应 index.html，
 * 由前端路由（react-router）接管。
 *
 * <p>关键点：每个路径段都用 {@code [^\\.]*} 排除点号，确保带扩展名的静态资源
 * （如 {@code /assets/index-xxx.js}、{@code /admin/assets/index-xxx.js}）不会被控制器拦截，
 * 而是落到 {@link WebConfig#addResourceHandlers} 注册的静态资源处理器。
 *
 * <p>注意：brief 原始模式 {@code /{path:[^\\.]*}/**} 与 {@code /admin/**} 的 {@code **} 尾部
 * 会匹配带点号的后续路径段，导致 (1) {@code /assets/index.js} 被当成 SPA 路由返回 index.html，
 * (2) admin forward 目标 {@code /admin/index.html} 被 {@code /admin/**} 再次匹配引发无限递归。
 * 故改为逐段点号排除的固定深度模式（覆盖现有前端路由：web 最深 2 段，admin 最深 2 段，+1 段余量）。
 */
@Controller
public class SpaForwardController {

  @Value("${app.static.web-dir:./dist/web}")
  private String webDir;
  @Value("${app.static.admin-dir:./dist/admin}")
  private String adminDir;

  // admin 未知子路由 → 回 /admin/index.html
  @GetMapping(value = {
      "/admin",
      "/admin/",
      "/admin/{a:[^\\.]*}",
      "/admin/{a:[^\\.]*}/{b:[^\\.]*}",
      "/admin/{a:[^\\.]*}/{b:[^\\.]*}/{c:[^\\.]*}"
  })
  public String adminForward() {
    return "forward:/admin/index.html";
  }

  // 根路径 web 未知路由 → 回 /index.html
  @GetMapping(value = {
      "/",
      "/{a:[^\\.]*}",
      "/{a:[^\\.]*}/{b:[^\\.]*}",
      "/{a:[^\\.]*}/{b:[^\\.]*}/{c:[^\\.]*}"
  })
  public String webForward() {
    return "forward:/index.html";
  }
}
