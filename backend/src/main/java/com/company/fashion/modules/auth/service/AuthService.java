package com.company.fashion.modules.auth.service;

import com.company.fashion.common.exception.BusinessException;
import com.company.fashion.modules.auth.dto.AuthTokenResponse;
import com.company.fashion.modules.auth.dto.LoginRequest;
import com.company.fashion.modules.auth.dto.RefreshTokenResponse;
import com.company.fashion.modules.auth.dto.RefreshRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final JwtService jwtService;

  @Value("${app.auth.username:stylist}")
  private String configuredUsername;

  @Value("${app.auth.password:stylist123}")
  private String configuredPassword;

  public AuthService(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  public AuthTokenResponse login(LoginRequest request) {
    if (!configuredUsername.equals(request.username()) || !configuredPassword.equals(request.password())) {
      throw new BusinessException(401, "Username or password incorrect");
    }

    String accessToken = jwtService.generateAccessToken(request.username());
    String refreshToken = jwtService.generateRefreshToken(request.username());
    return new AuthTokenResponse(accessToken, refreshToken, jwtService.getAccessExpirationSeconds());
  }

  public RefreshTokenResponse refresh(RefreshRequest request) {
    String username = jwtService.parseRefreshToken(request.refreshToken());
    String accessToken = jwtService.generateAccessToken(username);
    return new RefreshTokenResponse(accessToken, jwtService.getAccessExpirationSeconds());
  }
}
