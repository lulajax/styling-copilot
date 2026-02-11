package com.company.fashion.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login request")
public record LoginRequest(
    @Schema(description = "Username", example = "stylist")
    @NotBlank String username,
    @Schema(description = "Password", example = "stylist123")
    @NotBlank String password
) {
}
