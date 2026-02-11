package com.company.fashion.modules.match.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One outfit recommendation (TOP + BOTTOM pair)")
public record OutfitRecommendationResponse(
    @Schema(description = "Outfit sequence number in this task result", example = "1")
    int outfitNo,
    @Schema(description = "Recommended TOP clothing ID", example = "12")
    Long topClothingId,
    @Schema(description = "Recommended BOTTOM clothing ID", example = "24")
    Long bottomClothingId,
    @Schema(description = "Outfit score", example = "92")
    int score,
    @Schema(description = "Recommendation reason")
    String reason,
    @Schema(description = "Preview for this outfit, null when preview is skipped")
    OutfitPreviewResponse preview,
    @Schema(description = "Warning for this outfit, null when no warning")
    String warning
) {
}
