package com.gmnl.orientation.user;

public record UserDto(Long id, String username, String name, UserRole role) {
  public static UserDto from(User u) {
    return new UserDto(u.getId(), u.getUsername(), u.getName(), u.getRole());
  }
}
