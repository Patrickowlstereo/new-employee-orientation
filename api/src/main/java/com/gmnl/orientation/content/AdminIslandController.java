package com.gmnl.orientation.content;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 后台小岛管理接口（仅 ADMIN）。/api/admin/** 已由 SecurityConfig 限定 hasRole("ADMIN")。
 */
@RestController
@RequestMapping("/api/admin/islands")
public class AdminIslandController {

  private final AdminContentService service;

  public AdminIslandController(AdminContentService service) {
    this.service = service;
  }

  @PostMapping
  public IslandDto create(@Valid @RequestBody IslandUpsertRequest req) {
    return service.createIsland(req);
  }

  @PutMapping("/{id}")
  public IslandDto update(@PathVariable Long id, @Valid @RequestBody IslandUpsertRequest req) {
    return service.updateIsland(id, req);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    service.deleteIsland(id);
  }
}
