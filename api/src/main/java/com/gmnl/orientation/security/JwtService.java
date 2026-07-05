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
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
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
