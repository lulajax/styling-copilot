package com.company.fashion.modules.match.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Task detail response")
public record MatchTaskResultResponse(
    @Schema(description = "Task ID")
    String taskId,
    @Schema(description = "Current task status")
    TaskStatus status,
    @Schema(description = "Strategy used for recommendation", example = "AI_ONLY")
    String strategyName,
    @Schema(description = "Outfit recommendations, populated when task succeeds")
    List<OutfitRecommendationResponse> outfits,
    @Deprecated
    @Schema(description = "Legacy flattened recommendation list derived from outfits", deprecated = true)
    List<MatchResultItemResponse> result,
    @Deprecated
    @Schema(description = "Legacy preview from first outfit", deprecated = true)
    OutfitPreviewResponse preview,
    @Schema(description = "Error message for failed task, or warning message for successful degraded preview")
    String errorMessage
) {
}
