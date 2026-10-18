package com.bankingsystem.security;

import com.bankingsystem.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for issuing and validating signed JWT tokens.
 */
@Service
public class JwtService {

  private static final Logger log = LoggerFactory.getLogger(JwtService.class);

  private final JwtProperties jwtProperties;
  private final SecretKey key;

  public JwtService(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
    this.key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
  }

  public String generateToken(Long userId, String email, String role, String jti) {
    Instant now = Instant.now();
    Instant expiry = now.plus(jwtProperties.getExpiresMinutes(), ChronoUnit.MINUTES);

    return Jwts.builder()
        .subject(email)
        .id(jti)
        .claim("userId", userId)
        .claim("role", role)
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiry))
        .signWith(key)
        .compact();
  }

  public Claims extractClaims(String token) {
    try {
      return Jwts.parser()
          .verifyWith(key)
          .build()
          .parseSignedClaims(token)
          .getPayload();
    } catch (JwtException | IllegalArgumentException ex) {
      log.debug("JWT verification failed: {}", ex.getMessage());
      return null;
    }
  }

  public String extractUsername(String token) {
    Claims claims = extractClaims(token);
    return claims != null ? claims.getSubject() : null;
  }

  public String extractJti(String token) {
    Claims claims = extractClaims(token);
    return claims != null ? claims.getId() : null;
  }

  public Long extractUserId(String token) {
    Claims claims = extractClaims(token);
    if (claims == null) return null;
    Object val = claims.get("userId");
    return val instanceof Number num ? num.longValue() : null;
  }

  public boolean isTokenExpired(String token) {
    Claims claims = extractClaims(token);
    if (claims == null) return true;
    return claims.getExpiration().before(new Date());
  }
}
