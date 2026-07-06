package com.gmnl.orientation.content;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocRepository extends JpaRepository<Doc, Long> {
  List<Doc> findByIslandIdAndActiveTrueOrderByOrderAsc(Long islandId);
  long countByIslandId(Long islandId);
}
