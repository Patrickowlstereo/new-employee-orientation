package com.gmnl.orientation.progress;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProgressRepository extends JpaRepository<Progress, ProgressId> {
  List<Progress> findByUserId(Long userId);
}
