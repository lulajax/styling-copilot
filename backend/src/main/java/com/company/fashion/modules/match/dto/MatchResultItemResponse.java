package com.company.fashion.modules.match.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One recommendation item in match result")
public record MatchResultItemResponse(
    @Schema(description = "Recommended clothing ID", example = "4")
    Long clothingId,
    @Schema(description = "Recommendation reason")
    String reason,
    @Schema(description = "Recommendation score", example = "88")
    int score
) {
}
