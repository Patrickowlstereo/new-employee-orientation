package com.gmnl.orientation.progress;

import com.gmnl.orientation.user.CurrentUserResolver;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

  private final ProgressService progressService;
  private final CurrentUserResolver currentUser;

  public ProgressController(ProgressService progressService, CurrentUserResolver currentUser) {
    this.progressService = progressService;
    this.currentUser = currentUser;
  }

  @GetMapping
  public ProgressAggregateDto getAggregate() {
    return progressService.getAggregate(currentUser.userId());
  }

  @PutMapping("/{docId}")
  public ProgressItemDto upsert(@PathVariable Long docId, @Valid @RequestBody UpsertProgressRequest req) {
    return progressService.upsert(currentUser.userId(), docId, req.status(), req.progressPct());
  }

  @PostMapping("/{docId}/complete")
  public ProgressItemDto complete(@PathVariable Long docId) {
    return progressService.complete(currentUser.userId(), docId);
  }
}
