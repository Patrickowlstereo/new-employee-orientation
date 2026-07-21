package com.gmnl.orientation.security;

import com.gmnl.orientation.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Service
public class JwtService {

  private final SecretKey key;
  private final Duration expiration;

  public JwtService(@Value("${app.jwt.secret}") String secret,
                    @Value("${app.jwt.expiration-hours:8}") long hours) {
    // 密钥不允许为空或过短:jjwt HMAC-SHA 要求 ≥32 字节,缺失时启动即失败,
    // 避免带默认弱密钥上线。
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException(
          "app.jwt.secret 未配置:请通过环境变量 JWT_SECRET 提供至少 32 字节的密钥");
    }
    byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
    if (secretBytes.length < 32) {
      throw new IllegalStateException(
          "app.jwt.secret 长度不足:HMAC-SHA 要求至少 32 字节,当前 " + secretBytes.length + " 字节");
    }
    this.key = Keys.hmacShaKeyFor(secretBytes);
    this.expiration = Duration.ofHours(hours);
  }

  public String generate(Long userId, String username, String name, UserRole role) {
    Date now = new Date();
    Date exp = new Date(now.getTime() + expiration.toMillis());
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .claim("username", username)
        .claim("name", name)
        .claim("role", role.name())
        .issuedAt(now)
        .expiration(exp)
        .signWith(key)
        .compact();
  }

  public Claims parse(String token) throws JwtException {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
