package com.company.fashion.modules.match.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Outfit preview generated after recommendation")
@JsonIgnoreProperties(ignoreUnknown = true)
public record OutfitPreviewResponse(
    @Schema(description = "Preview title", example = "Casual Korean Daily Live Look")
    String title,
    @Schema(description = "Styling summary for this outfit")
    String outfitDescription,
    @Schema(description = "Prompt for rendering a try-on composite with member photo and clothing images")
    String imagePrompt
) {
}
