package com.gmnl.orientation.progress;

import jakarta.persistence.*;

@Entity
@Table(name = "island_states")
@IdClass(IslandStateId.class)
public class IslandState {
  @Id
  @Column(name = "user_id")
  private Long userId;

  @Id
  @Column(name = "island_id")
  private Long islandId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private IslandStatus status = IslandStatus.LOCKED;

  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Long getIslandId() { return islandId; }
  public void setIslandId(Long islandId) { this.islandId = islandId; }
  public IslandStatus getStatus() { return status; }
  public void setStatus(IslandStatus status) { this.status = status; }
}
