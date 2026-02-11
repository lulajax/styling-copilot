package com.company.fashion.modules.match.dto;

import com.company.fashion.modules.match.entity.MatchRecordStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Match history record")
public record MatchHistoryItemResponse(
    @Schema(description = "History record ID")
    Long id,
    @Schema(description = "Member ID")
    Long memberId,
    @Schema(description = "Member name")
    String memberName,
    @Schema(description = "Clothing ID")
    Long clothingId,
    @Schema(description = "Clothing name")
    String clothingName,
    @Schema(description = "Record status")
    MatchRecordStatus status,
    @Schema(description = "Performance score")
    Integer performanceScore,
    @Schema(description = "Broadcast date")
    LocalDateTime broadcastDate
) {
}
