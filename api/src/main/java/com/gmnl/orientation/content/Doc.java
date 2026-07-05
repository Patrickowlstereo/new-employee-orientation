package com.gmnl.orientation.content;

import jakarta.persistence.*;

@Entity
@Table(name = "docs")
public class Doc {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 128)
  private String title;

  @Column(length = 64)
  private String category;

  @Column(name = "institution_id", nullable = false)
  private Long institutionId;

  @Column(name = "island_id", nullable = false)
  private Long islandId;

  @Column(nullable = false)
  private Boolean required = false;

  @Column(name = "file_path", length = 256)
  private String filePath;

  @Column(name = "file_type", length = 8)
  private String fileType;

  @Column(name = "\"order\"", nullable = false)
  private Integer order = 0;

  @Column(nullable = false)
  private Boolean active = true;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getCategory() { return category; }
  public void setCategory(String category) { this.category = category; }
  public Long getInstitutionId() { return institutionId; }
  public void setInstitutionId(Long institutionId) { this.institutionId = institutionId; }
  public Long getIslandId() { return islandId; }
  public void setIslandId(Long islandId) { this.islandId = islandId; }
  public Boolean getRequired() { return required; }
  public void setRequired(Boolean required) { this.required = required; }
  public String getFilePath() { return filePath; }
  public void setFilePath(String filePath) { this.filePath = filePath; }
  public String getFileType() { return fileType; }
  public void setFileType(String fileType) { this.fileType = fileType; }
  public Integer getOrder() { return order; }
  public void setOrder(Integer order) { this.order = order; }
  public Boolean getActive() { return active; }
  public void setActive(Boolean active) { this.active = active; }
}
