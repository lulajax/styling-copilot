package com.company.fashion.modules.match.ai;

import com.company.fashion.modules.clothing.entity.Clothing;
import com.company.fashion.modules.match.dto.OutfitPreviewResponse;
import com.company.fashion.modules.match.entity.MatchRecord;
import com.company.fashion.modules.member.entity.Member;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AiClientRouter {

  private final GeminiClient geminiClient;
  private final OpenaiClient openaiClient;

  @Value("${app.ai.provider:gemini}")
  private String provider;

  public AiClientRouter(GeminiClient geminiClient, OpenaiClient openaiClient) {
    this.geminiClient = geminiClient;
    this.openaiClient = openaiClient;
  }

  public List<AiOutfitSuggestion> suggest(
      Member member,
      List<Clothing> candidates,
      List<MatchRecord> history,
      String scene,
      AiLanguage language
  ) {
    if (isOpenaiProvider()) {
      return openaiClient.suggest(member, candidates, history, scene, language);
    }
    return geminiClient.suggest(member, candidates, history, scene, language);
  }

  public OutfitPreviewResponse generateOutfitPreview(
      Member member,
      List<Clothing> selected,
      String scene,
      AiLanguage language
  ) {
    if (isOpenaiProvider()) {
      return openaiClient.generateOutfitPreview(member, selected, scene, language);
    }
    return geminiClient.generateOutfitPreview(member, selected, scene, language);
  }

  private boolean isOpenaiProvider() {
    return "openai".equalsIgnoreCase(valueOrDefault(provider));
  }

  private String valueOrDefault(String raw) {
    if (raw == null) {
      return "gemini";
    }
    return raw.trim().toLowerCase(Locale.ROOT);
  }

  public record AiOutfitSuggestion(Long topClothingId, Long bottomClothingId, int score, String reason) {
  }
}
