package com.company.fashion.modules.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Member response")
public record MemberResponse(
    @Schema(description = "Member ID", example = "1")
    Long id,
    @Schema(description = "Member name")
    String name,
    @Schema(description = "Body profile JSON (BodyProfile V2)")
    String bodyData,
    @Schema(description = "Member photo URL")
    String photoUrl,
    @Schema(description = "Comma separated style tags")
    String styleTags
) {
}
