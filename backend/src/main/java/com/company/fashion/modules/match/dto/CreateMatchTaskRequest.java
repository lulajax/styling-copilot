package com.company.fashion.modules.match.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Create match task request")
public record CreateMatchTaskRequest(
    @Schema(description = "Member ID", example = "1")
    @NotNull Long memberId,
    @Schema(description = "Candidate clothing IDs, max 20", example = "[1,2,4]")
    @NotEmpty @Size(max = 20) List<Long> clothingIds,
    @Schema(description = "Scene label", example = "daily-live")
    String scene
) {
}
