package com.company.fashion.modules.auth.service;

import com.company.fashion.common.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  @Value("${app.jwt.secret}")
  private String secret;

  @Value("${app.jwt.access-expiration-seconds:1800}")
  private long accessExpirationSeconds;

  @Value("${app.jwt.refresh-expiration-seconds:604800}")
  private long refreshExpirationSeconds;

  private SecretKey key;

  @PostConstruct
  void init() {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  public String generateAccessToken(String username) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(username)
        .claim("tokenType", "ACCESS")
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(accessExpirationSeconds)))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  public String generateRefreshToken(String username) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(username)
        .claim("tokenType", "REFRESH")
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(refreshExpirationSeconds)))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  public String parseAccessToken(String token) {
    Claims claims = parse(token);
    if (!"ACCESS".equals(claims.get("tokenType", String.class))) {
      throw new BusinessException(401, "Invalid access token");
    }
    return claims.getSubject();
  }

  public String parseRefreshToken(String token) {
    Claims claims = parse(token);
    if (!"REFRESH".equals(claims.get("tokenType", String.class))) {
      throw new BusinessException(401, "Invalid refresh token");
    }
    return claims.getSubject();
  }

  public long getAccessExpirationSeconds() {
    return accessExpirationSeconds;
  }

  private Claims parse(String token) {
    try {
      Jws<Claims> jws = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
      return jws.getPayload();
    } catch (JwtException | IllegalArgumentException ex) {
      throw new BusinessException(401, "Token verification failed");
    }
  }
}
