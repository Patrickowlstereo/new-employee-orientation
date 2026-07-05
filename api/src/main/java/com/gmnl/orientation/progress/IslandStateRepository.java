package com.gmnl.orientation.progress;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IslandStateRepository extends JpaRepository<IslandState, IslandStateId> {
  List<IslandState> findByUserId(Long userId);
}
