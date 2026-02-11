package com.company.fashion.modules.match.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Schema(description = "Manual history create request")
public record CreateManualHistoryRequest(
    @Schema(description = "Clothing ID", example = "1")
    @NotNull Long clothingId,
    @Schema(description = "Actual broadcast datetime, defaults to now when omitted")
    LocalDateTime broadcastDate,
    @Schema(description = "Optional performance score", example = "85")
    Integer performanceScore
) {
}
