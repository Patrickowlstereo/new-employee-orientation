package com.gmnl.orientation.content;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 后台机构管理接口（仅 ADMIN）。/api/admin/** 已由 SecurityConfig 限定 hasRole("ADMIN")。
 */
@RestController
@RequestMapping("/api/admin/institutions")
public class AdminInstitutionController {

  private final AdminContentService service;

  public AdminInstitutionController(AdminContentService service) {
    this.service = service;
  }

  @PostMapping
  public InstitutionDto create(@Valid @RequestBody InstitutionUpsertRequest req) {
    return service.createInstitution(req);
  }

  @PutMapping("/{id}")
  public InstitutionDto update(@PathVariable Long id, @Valid @RequestBody InstitutionUpsertRequest req) {
    return service.updateInstitution(id, req);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    service.deleteInstitution(id);
  }
}
