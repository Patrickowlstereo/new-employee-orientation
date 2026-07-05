package com.gmnl.orientation.progress;

import java.io.Serializable;
import java.util.Objects;

public class IslandStateId implements Serializable {
  private Long userId;
  private Long islandId;

  public IslandStateId() {}
  public IslandStateId(Long userId, Long islandId) {
    this.userId = userId;
    this.islandId = islandId;
  }

  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Long getIslandId() { return islandId; }
  public void setIslandId(Long islandId) { this.islandId = islandId; }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof IslandStateId that)) return false;
    return Objects.equals(userId, that.userId) && Objects.equals(islandId, that.islandId);
  }

  @Override
  public int hashCode() { return Objects.hash(userId, islandId); }
}
