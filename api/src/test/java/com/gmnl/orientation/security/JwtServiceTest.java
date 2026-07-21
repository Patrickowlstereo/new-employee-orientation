package com.gmnl.orientation.security;

import com.gmnl.orientation.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

  private final JwtService service = new JwtService(
      "test-secret-test-secret-test-secret-test-secret-32b", 8);

  @Test
  void generateThenParseRoundTrip() {
    String token = service.generate(42L, "zhangsan", "张三", UserRole.USER);
    Claims c = service.parse(token);
    assertEquals("42", c.getSubject());
    assertEquals("zhangsan", c.get("username", String.class));
    assertEquals("USER", c.get("role", String.class));
  }

  @Test
  void invalidTokenThrows() {
    assertThrows(JwtException.class, () -> service.parse("not-a-token"));
  }

  @Test
  void blankSecretRejectedAtStartup() {
    assertThrows(IllegalStateException.class, () -> new JwtService("", 8));
    assertThrows(IllegalStateException.class, () -> new JwtService("   ", 8));
  }

  @Test
  void shortSecretRejectedAtStartup() {
    assertThrows(IllegalStateException.class, () -> new JwtService("too-short", 8));
  }
}
