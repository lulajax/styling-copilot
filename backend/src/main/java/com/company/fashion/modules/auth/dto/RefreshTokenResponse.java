package com.company.fashion.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Refresh token response")
public record RefreshTokenResponse(
    @Schema(description = "New access token")
    String accessToken,
    @Schema(description = "Access token expiration seconds", example = "1800")
    long expiresIn
) {
}
