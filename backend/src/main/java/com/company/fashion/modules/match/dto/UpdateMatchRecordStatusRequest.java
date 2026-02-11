package com.company.fashion.modules.match.dto;

import com.company.fashion.modules.match.entity.MatchRecordStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Update match history record status request")
public record UpdateMatchRecordStatusRequest(
    @Schema(description = "Target status", example = "BROADCASTED")
    @NotNull MatchRecordStatus status
) {
}
