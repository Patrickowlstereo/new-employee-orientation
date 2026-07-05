package com.gmnl.orientation.content;

import jakarta.persistence.*;

@Entity
@Table(name = "institutions")
public class Institution {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 8)
  private String key;

  @Column(nullable = false, length = 32)
  private String name;

  @Column(name = "\"order\"", nullable = false)
  private Integer order = 0;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getKey() { return key; }
  public void setKey(String key) { this.key = key; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public Integer getOrder() { return order; }
  public void setOrder(Integer order) { this.order = order; }
}
