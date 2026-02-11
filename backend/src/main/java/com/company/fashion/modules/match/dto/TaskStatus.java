package com.company.fashion.modules.match.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Task status enum")
public enum TaskStatus {
  @Schema(description = "Task is queued")
  QUEUED,
  @Schema(description = "Task is running")
  RUNNING,
  @Schema(description = "Task completed successfully")
  SUCCEEDED,
  @Schema(description = "Task completed with failure")
  FAILED
}
