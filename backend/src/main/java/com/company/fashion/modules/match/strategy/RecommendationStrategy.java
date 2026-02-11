package com.company.fashion.modules.match.strategy;

import com.company.fashion.modules.match.dto.OutfitRecommendationResponse;
import java.util.List;

public interface RecommendationStrategy {

  String strategyName();

  boolean supports(boolean coldStart);

  RecommendationDecision recommend(RecommendationRequest request);

  record RecommendationDecision(List<OutfitRecommendationResponse> outfits, String warning) {
  }
}
