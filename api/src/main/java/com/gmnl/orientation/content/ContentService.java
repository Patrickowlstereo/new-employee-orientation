package com.gmnl.orientation.content;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ContentService {

  private final InstitutionRepository institutionRepo;
  private final IslandRepository islandRepo;
  private final DocRepository docRepo;

  public ContentService(InstitutionRepository institutionRepo, IslandRepository islandRepo, DocRepository docRepo) {
    this.institutionRepo = institutionRepo;
    this.islandRepo = islandRepo;
    this.docRepo = docRepo;
  }

  public List<InstitutionDto> listInstitutions() {
    return institutionRepo.findAllByOrderByOrderAsc().stream().map(inst -> {
      List<IslandDto> islands = islandRepo.findByInstitutionIdOrderByOrderAsc(inst.getId())
          .stream().map(IslandDto::from).toList();
      return new InstitutionDto(inst.getId(), inst.getKey(), inst.getName(), inst.getOrder(), islands);
    }).toList();
  }

  public List<IslandDto> listIslands(Long institutionId) {
    if (institutionId != null) {
      return islandRepo.findByInstitutionIdOrderByOrderAsc(institutionId).stream().map(IslandDto::from).toList();
    }
    return islandRepo.findAllByOrderByOrderAsc().stream().map(IslandDto::from).toList();
  }

  public List<DocDto> listDocs(Long islandId, Boolean required) {
    List<Doc> docs = (islandId != null)
        ? docRepo.findByIslandIdAndActiveTrueOrderByOrderAsc(islandId)
        : docRepo.findAll().stream().filter(Doc::getActive).sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder())).toList();
    return docs.stream()
        .filter(d -> required == null || d.getRequired().equals(required))
        .map(DocDto::from)
        .toList();
  }

  public Doc getDocEntity(Long id) {
    return docRepo.findById(id)
        .filter(Doc::getActive)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "文档不存在"));
  }
}
