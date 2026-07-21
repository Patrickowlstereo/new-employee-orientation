package com.gmnl.orientation.user;

import com.gmnl.orientation.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

  private UserRepository userRepo;
  private LoginAttemptService loginAttemptService;
  private AuthService authService;

  @BeforeEach
  void setup() {
    userRepo = mock(UserRepository.class);
    var encoder = new BCryptPasswordEncoder(4); // 测试用低 cost 提速
    var jwt = new JwtService("test-secret-test-secret-test-secret-test-secret-32b", 8);
    loginAttemptService = new LoginAttemptService(5, Duration.ofMinutes(10), Clock.systemUTC());
    authService = new AuthService(userRepo, encoder, jwt, loginAttemptService);
  }

  private User adminUser() {
    User u = new User();
    u.setId(1L);
    u.setUsername("admin");
    u.setName("管理员");
    u.setRole(UserRole.ADMIN);
    u.setPasswordHash(new BCryptPasswordEncoder(4).encode("admin12345"));
    return u;
  }

  @Test
  void loginWithCorrectPasswordSucceeds() {
    User u = adminUser();
    when(userRepo.findByUsername("admin")).thenReturn(Optional.of(u));

    var result = authService.login("admin", "admin12345");
    assertEquals("admin", result.user().username());
    assertNotNull(result.token());
  }

  @Test
  void loginWithWrongPasswordThrowsInvalid() {
    User u = adminUser();
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

  @Test
  void fiveFailuresThenLocked() {
    User u = adminUser();
    when(userRepo.findByUsername("admin")).thenReturn(Optional.of(u));

    for (int i = 0; i < 5; i++) {
      assertThrows(AuthService.InvalidCredentialsException.class,
          () -> authService.login("admin", "wrong"));
    }
    // 第 6 次:已锁定,即使密码错误也直接返回锁定错误
    assertThrows(AuthService.LoginLockedException.class,
        () -> authService.login("admin", "wrong"));
  }

  @Test
  void successResetsFailureCounter() {
    User u = adminUser();
    when(userRepo.findByUsername("admin")).thenReturn(Optional.of(u));

    for (int i = 0; i < 4; i++) {
      assertThrows(AuthService.InvalidCredentialsException.class,
          () -> authService.login("admin", "wrong"));
    }
    authService.login("admin", "admin12345"); // 成功,计数清零

    // 再失败 4 次仍不锁定
    for (int i = 0; i < 4; i++) {
      assertThrows(AuthService.InvalidCredentialsException.class,
          () -> authService.login("admin", "wrong"));
    }
    assertTrue(loginAttemptService.isLocked("admin") == false);
  }

  @Test
  void lockedAccountRejectsEvenCorrectPassword() {
    User u = adminUser();
    when(userRepo.findByUsername("admin")).thenReturn(Optional.of(u));

    for (int i = 0; i < 5; i++) {
      assertThrows(AuthService.InvalidCredentialsException.class,
          () -> authService.login("admin", "wrong"));
    }
    assertThrows(AuthService.LoginLockedException.class,
        () -> authService.login("admin", "admin12345"));
  }

  @Test
  void lockExpiresAfterDuration() {
    // 可控时钟:锁定时长 10 分钟
    AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2024-01-01T00:00:00Z"));
    Clock mutableClock = new Clock() {
      @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
      @Override public Clock withZone(java.time.ZoneId zone) { return this; }
      @Override public Instant instant() { return now.get(); }
    };
    LoginAttemptService attempts = new LoginAttemptService(2, Duration.ofMinutes(10), mutableClock);
    assertFalse(attempts.isLocked("a"));
    attempts.onFailure("a");
    assertFalse(attempts.isLocked("a"));
    attempts.onFailure("a");
    assertTrue(attempts.isLocked("a"));
    now.set(now.get().plus(Duration.ofMinutes(11)));
    assertFalse(attempts.isLocked("a"));
  }
}
