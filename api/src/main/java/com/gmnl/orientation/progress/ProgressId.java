package com.gmnl.orientation.progress;

import java.io.Serializable;
import java.util.Objects;

public class ProgressId implements Serializable {
  private Long userId;
  private Long docId;

  public ProgressId() {}
  public ProgressId(Long userId, Long docId) {
    this.userId = userId;
    this.docId = docId;
  }

  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Long getDocId() { return docId; }
  public void setDocId(Long docId) { this.docId = docId; }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ProgressId that)) return false;
    return Objects.equals(userId, that.userId) && Objects.equals(docId, that.docId);
  }

  @Override
  public int hashCode() { return Objects.hash(userId, docId); }
}
