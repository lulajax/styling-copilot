package com.company.fashion.modules.match.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Member match history response")
public record MatchHistoryResponse(
    @Schema(description = "History records")
    List<MatchHistoryItemResponse> records,
    @Schema(description = "Total records count")
    long total
) {
}
