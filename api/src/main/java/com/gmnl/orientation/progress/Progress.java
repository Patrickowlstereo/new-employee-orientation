package com.gmnl.orientation.progress;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "progress")
@IdClass(ProgressId.class)
public class Progress {
  @Id
  @Column(name = "user_id")
  private Long userId;

  @Id
  @Column(name = "doc_id")
  private Long docId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private ProgressStatus status = ProgressStatus.NOT_STARTED;

  @Column(name = "progress_pct", nullable = false)
  private Integer progressPct = 0;

  @Column(name = "last_read_at")
  private Instant lastReadAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Long getDocId() { return docId; }
  public void setDocId(Long docId) { this.docId = docId; }
  public ProgressStatus getStatus() { return status; }
  public void setStatus(ProgressStatus status) { this.status = status; }
  public Integer getProgressPct() { return progressPct; }
  public void setProgressPct(Integer progressPct) { this.progressPct = progressPct; }
  public Instant getLastReadAt() { return lastReadAt; }
  public void setLastReadAt(Instant lastReadAt) { this.lastReadAt = lastReadAt; }
  public Instant getCompletedAt() { return completedAt; }
  public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
