package com.gmnl.orientation.content;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IslandRepository extends JpaRepository<Island, Long> {
  List<Island> findByInstitutionIdOrderByOrderAsc(Long institutionId);
  List<Island> findAllByOrderByOrderAsc();
  long countByInstitutionId(Long institutionId);
}
