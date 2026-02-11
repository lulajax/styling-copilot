package com.company.fashion.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Login token response")
public record AuthTokenResponse(
    @Schema(description = "Access token")
    String accessToken,
    @Schema(description = "Refresh token")
    String refreshToken,
    @Schema(description = "Access token expiration seconds", example = "1800")
    long expiresIn
) {
}
