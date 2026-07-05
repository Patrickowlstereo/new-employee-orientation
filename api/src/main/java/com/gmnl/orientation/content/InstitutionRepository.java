package com.gmnl.orientation.content;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InstitutionRepository extends JpaRepository<Institution, Long> {
  List<Institution> findAllByOrderByOrderAsc();
}
