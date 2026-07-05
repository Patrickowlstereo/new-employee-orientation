package com.gmnl.orientation.progress;

import java.time.Instant;

public record ProgressItemDto(Long docId, ProgressStatus status, Integer progressPct,
                              Instant lastReadAt, Instant completedAt) {
  public static ProgressItemDto from(Progress p) {
    return new ProgressItemDto(p.getDocId(), p.getStatus(), p.getProgressPct(),
        p.getLastReadAt(), p.getCompletedAt());
  }
}
