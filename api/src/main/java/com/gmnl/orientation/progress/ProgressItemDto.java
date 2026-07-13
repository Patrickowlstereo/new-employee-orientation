package com.gmnl.orientation.progress;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record ProgressItemDto(Long docId, ProgressStatus status, Integer progressPct,
                              @Schema(nullable = true) Instant lastReadAt,
                              @Schema(nullable = true) Instant completedAt) {
  public static ProgressItemDto from(Progress p) {
    return new ProgressItemDto(p.getDocId(), p.getStatus(), p.getProgressPct(),
        p.getLastReadAt(), p.getCompletedAt());
  }
}
