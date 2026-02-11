package com.company.fashion.modules.match.strategy;

import com.company.fashion.modules.clothing.entity.Clothing;
import com.company.fashion.modules.match.ai.AiLanguage;
import com.company.fashion.modules.match.dto.OutfitRecommendationResponse;
import com.company.fashion.modules.match.entity.MatchRecord;
import com.company.fashion.modules.member.entity.Member;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {

  private static final String AI_ONLY_STRATEGY = "AI_ONLY";
  private final List<RecommendationStrategy> strategies;

  public RecommendationService(List<RecommendationStrategy> strategies) {
    this.strategies = strategies;
  }

  public RecommendationOutput recommend(
      Member member,
      List<Clothing> candidates,
      List<MatchRecord> history,
      String scene,
      AiLanguage language
  ) {
    RecommendationStrategy strategy = strategies.stream()
        .filter(item -> AI_ONLY_STRATEGY.equals(item.strategyName()))
        .filter(item -> item.supports(true))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No AI recommendation strategy configured"));

    RecommendationRequest request = new RecommendationRequest(member, candidates, history, scene, language);
    RecommendationStrategy.RecommendationDecision decision = strategy.recommend(request);
    return new RecommendationOutput(strategy.strategyName(), decision.outfits(), decision.warning());
  }

  public record RecommendationOutput(String strategyName, List<OutfitRecommendationResponse> outfits, String warning) {
  }
}
