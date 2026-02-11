package com.company.fashion.modules.clothing.dto;

import com.company.fashion.modules.clothing.entity.ClothingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Update clothing status request")
public record UpdateClothingStatusRequest(
    @Schema(description = "New status", example = "OFF_SHELF")
    @NotNull ClothingStatus status
) {
}
