package com.company.fashion.modules.match.service;

import com.company.fashion.modules.clothing.entity.Clothing;
import com.company.fashion.modules.match.ai.AiLanguage;
import com.company.fashion.modules.match.ai.AiClientRouter;
import com.company.fashion.modules.match.dto.OutfitPreviewResponse;
import com.company.fashion.modules.member.entity.Member;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OutfitPreviewService {

  public static final String PREVIEW_DEGRADED_WARNING = "Preview skipped: missing member photo or clothing image";
  private final AiClientRouter aiClientRouter;

  public OutfitPreviewService(AiClientRouter aiClientRouter) {
    this.aiClientRouter = aiClientRouter;
  }

  public OutfitPreviewDecision generate(Member member, List<Clothing> selected, String scene, AiLanguage language) {
    if (shouldSkipPreview(member, selected)) {
      return new OutfitPreviewDecision(null, PREVIEW_DEGRADED_WARNING);
    }

    OutfitPreviewResponse aiPreview = aiClientRouter.generateOutfitPreview(member, selected, scene, language);
    if (aiPreview == null || aiPreview.title() == null || aiPreview.title().isBlank()
        || aiPreview.outfitDescription() == null || aiPreview.outfitDescription().isBlank()
        || aiPreview.imagePrompt() == null || aiPreview.imagePrompt().isBlank()) {
      throw new IllegalStateException("AI preview generation unavailable");
    }
    return new OutfitPreviewDecision(aiPreview, null);
  }

  private boolean shouldSkipPreview(Member member, List<Clothing> selected) {
    if (member == null || member.getPhotoUrl() == null || member.getPhotoUrl().isBlank()) {
      return true;
    }
    if (selected == null || selected.isEmpty()) {
      return true;
    }
    for (Clothing clothing : selected) {
      if (clothing == null || clothing.getImageUrl() == null || clothing.getImageUrl().isBlank()) {
        return true;
      }
    }
    return false;
  }

  public record OutfitPreviewDecision(OutfitPreviewResponse preview, String warning) {
  }
}
