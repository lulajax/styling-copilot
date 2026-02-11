package com.company.fashion.modules.match.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Create task response")
public record CreateMatchTaskResponse(
    @Schema(description = "Task ID")
    String taskId,
    @Schema(description = "Initial task status")
    TaskStatus status
) {
}
