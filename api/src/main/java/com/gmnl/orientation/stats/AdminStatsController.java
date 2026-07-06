package com.gmnl.orientation.stats;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台统计接口（仅 ADMIN）。/api/admin/** 已由 SecurityConfig 限定 hasRole("ADMIN")。
 */
@RestController
@RequestMapping("/api/admin/stats")
public class AdminStatsController {

  private final StatsService service;

  public AdminStatsController(StatsService service) {
    this.service = service;
  }

  /** 全员学习明细（每员工完成率）。 */
  @GetMapping("/users")
  public List<UserStatsDto> users() {
    return service.userStats();
  }

  /** 概览：总人数/完成情况/各小岛完成率。 */
  @GetMapping("/overview")
  public StatsOverviewDto overview() {
    return service.overview();
  }
}
