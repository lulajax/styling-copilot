package com.company.fashion.modules.clothing.dto;

import com.company.fashion.modules.clothing.entity.ClothingStatus;
import com.company.fashion.modules.clothing.entity.ClothingType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Create clothing request")
public record CreateClothingRequest(
    @Schema(description = "Clothing name", example = "White T-Shirt")
    @NotBlank String name,
    @Schema(description = "Image URL")
    String imageUrl,
    @Schema(description = "Comma separated style tags", example = "casual,summer")
    String styleTags,
    @Schema(description = "Clothing type (only TOP/BOTTOM accepted for write)", example = "TOP")
    @NotNull ClothingType clothingType,
    @Schema(description = "Inventory status", example = "ON_SHELF")
    ClothingStatus status,
    @Schema(description = "Size data JSON string", example = "{\"shoulderWidthCm\":38,\"bustCm\":96,\"waistCm\":88,\"lengthCm\":62}")
    String sizeData
) {
}
