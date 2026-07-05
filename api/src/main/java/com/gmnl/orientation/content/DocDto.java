package com.gmnl.orientation.content;

public record DocDto(Long id, String title, String category, Long institutionId, Long islandId,
                     Boolean required, String fileType, Integer order, Boolean active) {
  public static DocDto from(Doc d) {
    return new DocDto(d.getId(), d.getTitle(), d.getCategory(), d.getInstitutionId(),
        d.getIslandId(), d.getRequired(), d.getFileType(), d.getOrder(), d.getActive());
  }
}
