package com.company.fashion.modules.clothing.dto;

import com.company.fashion.modules.clothing.entity.ClothingStatus;
import com.company.fashion.modules.clothing.entity.ClothingType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Clothing item response")
public record ClothingItemResponse(
    @Schema(description = "Clothing ID", example = "1")
    Long id,
    @Schema(description = "Clothing name")
    String name,
    @Schema(description = "Image URL")
    String imageUrl,
    @Schema(description = "Comma separated style tags")
    String styleTags,
    @Schema(description = "Clothing type")
    ClothingType clothingType,
    @Schema(description = "Inventory status")
    ClothingStatus status,
    @Schema(description = "Size data JSON string")
    String sizeData
) {
}
