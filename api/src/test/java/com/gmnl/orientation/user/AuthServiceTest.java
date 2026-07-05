package com.gmnl.orientation.user;

import com.gmnl.orientation.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

  private UserRepository userRepo;
  private AuthService authService;

  @BeforeEach
  void setup() {
    userRepo = mock(UserRepository.class);
    var encoder = new BCryptPasswordEncoder(4); // 测试用低 cost 提速
    var jwt = new JwtService("test-secret-test-secret-test-secret-test-secret-32b", 8);
    authService = new AuthService(userRepo, encoder, jwt);
  }

  @Test
  void loginWithCorrectPasswordSucceeds() {
    User u = new User();
    u.setId(1L);
    u.setUsername("admin");
    u.setName("管理员");
    u.setRole(UserRole.ADMIN);
    u.setPasswordHash(new BCryptPasswordEncoder(4).encode("admin12345"));
    when(userRepo.findByUsername("admin")).thenReturn(Optional.of(u));

    var result = authService.login("admin", "admin12345");
    assertEquals("admin", result.user().username());
    assertNotNull(result.token());
  }

  @Test
  void loginWithWrongPasswordThrowsInvalid() {
    User u = new User();
    u.setUsername("admin");
    u.setPasswordHash(new BCryptPasswordEncoder(4).encode("admin12345"));
    when(userRepo.findByUsername("admin")).thenReturn(Optional.of(u));

    assertThrows(AuthService.InvalidCredentialsException.class,
        () -> authService.login("admin", "wrong"));
  }

  @Test
  void loginWithUnknownUserThrowsInvalid() {
    when(userRepo.findByUsername("nobody")).thenReturn(Optional.empty());
    assertThrows(AuthService.InvalidCredentialsException.class,
        () -> authService.login("nobody", "whatever"));
  }
}
