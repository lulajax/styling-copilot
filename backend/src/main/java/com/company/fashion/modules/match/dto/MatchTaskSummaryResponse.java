package com.company.fashion.modules.match.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Task summary item")
public record MatchTaskSummaryResponse(
    @Schema(description = "Task ID")
    String taskId,
    @Schema(description = "Member ID")
    Long memberId,
    @Schema(description = "Scene label")
    String scene,
    @Schema(description = "Task status")
    TaskStatus status,
    @Schema(description = "Strategy name")
    String strategyName,
    @Schema(description = "Task creation time")
    LocalDateTime createdAt
) {
}
