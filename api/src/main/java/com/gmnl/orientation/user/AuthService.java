package com.gmnl.orientation.user;

import com.gmnl.orientation.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserRepository userRepo;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthService(UserRepository userRepo, PasswordEncoder passwordEncoder, JwtService jwtService) {
    this.userRepo = userRepo;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  public record LoginResult(String token, UserDto user) {}

  public LoginResult login(String username, String password) {
    User user = userRepo.findByUsername(username)
        .orElseThrow(() -> invalidCredentials());
    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      throw invalidCredentials();
    }
    String token = jwtService.generate(user.getId(), user.getUsername(), user.getName(), user.getRole());
    return new LoginResult(token, UserDto.from(user));
  }

  @Transactional
  public void changePassword(Long userId, String oldPassword, String newPassword) {
    User user = userRepo.findById(userId)
        .orElseThrow(() -> invalidCredentials());
    if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
      throw invalidCredentials();
    }
    user.setPasswordHash(passwordEncoder.encode(newPassword));
    userRepo.save(user);
  }

  private RuntimeException invalidCredentials() {
    return new InvalidCredentialsException();
  }

  public static class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() { super("账号或密码错误"); }
  }
}
