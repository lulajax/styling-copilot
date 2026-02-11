package com.company.fashion.modules.clothing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Paged clothing pool response")
public record ClothingPageResponse(
    @Schema(description = "Clothing items")
    List<ClothingItemResponse> items,
    @Schema(description = "Total count", example = "23")
    long total
) {
}
