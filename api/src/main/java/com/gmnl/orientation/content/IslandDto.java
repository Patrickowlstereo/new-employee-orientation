package com.gmnl.orientation.content;

public record IslandDto(Long id, String key, String name, Integer order, Long institutionId) {
  public static IslandDto from(Island i) {
    return new IslandDto(i.getId(), i.getKey(), i.getName(), i.getOrder(), i.getInstitutionId());
  }
}
