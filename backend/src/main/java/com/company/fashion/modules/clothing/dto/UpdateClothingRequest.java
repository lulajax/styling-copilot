package com.company.fashion.modules.clothing.dto;

import com.company.fashion.modules.clothing.entity.ClothingType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Update clothing request")
public record UpdateClothingRequest(
    @Schema(description = "Clothing name")
    String name,
    @Schema(description = "Image URL")
    String imageUrl,
    @Schema(description = "Comma separated style tags")
    String styleTags,
    @Schema(description = "Clothing type (only TOP/BOTTOM accepted for write)", example = "TOP")
    @NotNull ClothingType clothingType,
    @Schema(description = "Size data JSON string", example = "{\"shoulderWidthCm\":38,\"bustCm\":96,\"waistCm\":88,\"lengthCm\":62}")
    String sizeData
) {
}
