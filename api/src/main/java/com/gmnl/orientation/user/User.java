package com.gmnl.orientation.user;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 64)
  private String username;

  @Column(nullable = false, length = 64)
  private String name;

  @Column(name = "password_hash", nullable = false, length = 100)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private UserRole role = UserRole.USER;

  @Column(name = "employee_no", length = 32)
  private String employeeNo;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  // getters & setters
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getPasswordHash() { return passwordHash; }
  public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
  public UserRole getRole() { return role; }
  public void setRole(UserRole role) { this.role = role; }
  public String getEmployeeNo() { return employeeNo; }
  public void setEmployeeNo(String employeeNo) { this.employeeNo = employeeNo; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
