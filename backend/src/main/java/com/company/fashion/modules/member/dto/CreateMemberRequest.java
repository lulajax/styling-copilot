package com.company.fashion.modules.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Create member request")
public record CreateMemberRequest(
    @Schema(description = "Member name", example = "DemoStreamerA")
    @NotBlank String name,
    @Schema(
        description = "Body profile JSON (BodyProfile V2)",
        example = "{\"version\":2,\"measurements\":{\"heightCm\":168.0,\"weightKg\":49.0,\"shoulderWidthCm\":38.0,"
            + "\"bustCm\":84.0,\"waistCm\":62.0,\"hipCm\":89.0,\"bodyShape\":\"X\",\"legRatio\":\"long\","
            + "\"topSize\":\"S\",\"bottomSize\":\"S\"}}"
    )
    String bodyData,
    @Schema(description = "Member photo URL")
    String photoUrl,
    @Schema(description = "Comma separated style tags", example = "casual,korean")
    String styleTags
) {
}
