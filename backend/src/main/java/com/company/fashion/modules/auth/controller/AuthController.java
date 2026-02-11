package com.company.fashion.modules.auth.controller;

import com.company.fashion.common.api.Result;
import com.company.fashion.modules.auth.dto.AuthTokenResponse;
import com.company.fashion.modules.auth.dto.LoginRequest;
import com.company.fashion.modules.auth.dto.RefreshRequest;
import com.company.fashion.modules.auth.dto.RefreshTokenResponse;
import com.company.fashion.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Authentication and token refresh APIs")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  @Operation(summary = "Login and get access/refresh token")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Login success"),
      @ApiResponse(responseCode = "401", description = "Username or password incorrect",
          content = @Content(schema = @Schema(implementation = Object.class)))
  })
  public Result<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
    return Result.ok(authService.login(request));
  }

  @PostMapping("/refresh")
  @Operation(summary = "Refresh access token with refresh token")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Refresh success"),
      @ApiResponse(responseCode = "401", description = "Refresh token invalid or expired",
          content = @Content(schema = @Schema(implementation = Object.class)))
  })
  public Result<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
    return Result.ok(authService.refresh(request));
  }
}
