package com.company.fashion.modules.match.strategy;

import com.company.fashion.modules.clothing.entity.Clothing;
import com.company.fashion.modules.clothing.entity.ClothingType;
import com.company.fashion.modules.match.ai.AiClientRouter;
import com.company.fashion.modules.match.dto.OutfitRecommendationResponse;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class HybridRecommendationStrategy implements RecommendationStrategy {

  private final AiClientRouter aiClientRouter;

  public HybridRecommendationStrategy(AiClientRouter aiClientRouter) {
    this.aiClientRouter = aiClientRouter;
  }

  @Override
  public String strategyName() {
    return "AI_ONLY";
  }

  @Override
  public boolean supports(boolean coldStart) {
    return true;
  }

  @Override
  public RecommendationDecision recommend(RecommendationRequest request) {
    List<AiClientRouter.AiOutfitSuggestion> aiSuggestions = aiClientRouter.suggest(
        request.member(),
        request.candidates(),
        request.history(),
        request.scene(),
        request.language()
    );

    Map<Long, Clothing> candidateMap = request.candidates().stream()
        .collect(Collectors.toMap(Clothing::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    List<AiClientRouter.AiOutfitSuggestion> rankedSuggestions = aiSuggestions.stream()
        .sorted(Comparator.comparingInt(AiClientRouter.AiOutfitSuggestion::score).reversed())
        .toList();

    List<OutfitRecommendationResponse> outfits = selectValidOutfits(candidateMap, rankedSuggestions);
    if (outfits.isEmpty()) {
      throw new IllegalStateException("AI outfit recommendation unavailable or invalid");
    }
    return new RecommendationDecision(outfits, null);
  }

  private List<OutfitRecommendationResponse> selectValidOutfits(
      Map<Long, Clothing> candidateMap,
      List<AiClientRouter.AiOutfitSuggestion> rankedSuggestions
  ) {
    List<OutfitRecommendationResponse> outfits = new java.util.ArrayList<>();

    for (AiClientRouter.AiOutfitSuggestion suggestion : rankedSuggestions) {
      Pair pair = resolvePair(candidateMap, suggestion);
      if (pair == null) {
        continue;
      }

      String reason = suggestion.reason() == null || suggestion.reason().isBlank()
          ? "AI outfit recommendation"
          : suggestion.reason().trim();
      outfits.add(new OutfitRecommendationResponse(
          outfits.size() + 1,
          pair.top().getId(),
          pair.bottom().getId(),
          Math.max(0, Math.min(100, suggestion.score())),
          reason,
          null,
          null
      ));
    }
    return outfits;
  }

  private Pair resolvePair(Map<Long, Clothing> candidateMap, AiClientRouter.AiOutfitSuggestion suggestion) {
    if (suggestion == null || suggestion.topClothingId() == null || suggestion.bottomClothingId() == null) {
      return null;
    }
    if (suggestion.topClothingId().equals(suggestion.bottomClothingId())) {
      return null;
    }

    Clothing topCandidate = candidateMap.get(suggestion.topClothingId());
    Clothing bottomCandidate = candidateMap.get(suggestion.bottomClothingId());
    if (topCandidate == null || bottomCandidate == null) {
      return null;
    }

    if (topCandidate.getClothingType() == ClothingType.TOP && bottomCandidate.getClothingType() == ClothingType.BOTTOM) {
      return new Pair(topCandidate, bottomCandidate);
    }
    if (topCandidate.getClothingType() == ClothingType.BOTTOM && bottomCandidate.getClothingType() == ClothingType.TOP) {
      return new Pair(bottomCandidate, topCandidate);
    }
    return null;
  }

  private record Pair(Clothing top, Clothing bottom) {
  }
}
