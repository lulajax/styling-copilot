package com.company.fashion.modules.match.strategy;

import com.company.fashion.modules.clothing.entity.Clothing;
import com.company.fashion.modules.clothing.entity.ClothingType;
import com.company.fashion.modules.match.dto.OutfitRecommendationResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class RuleRecommendationStrategy implements RecommendationStrategy {

  @Override
  public String strategyName() {
    return "RULE_BASED";
  }

  @Override
  public boolean supports(boolean coldStart) {
    return coldStart;
  }

  @Override
  public RecommendationDecision recommend(RecommendationRequest request) {
    Map<Long, Integer> scores = buildBaseScores(request);
    List<Clothing> tops = request.candidates().stream()
        .filter(item -> item.getClothingType() == ClothingType.TOP)
        .sorted(Comparator.comparingInt((Clothing item) -> scores.getOrDefault(item.getId(), 60)).reversed())
        .toList();
    List<Clothing> bottoms = request.candidates().stream()
        .filter(item -> item.getClothingType() == ClothingType.BOTTOM)
        .sorted(Comparator.comparingInt((Clothing item) -> scores.getOrDefault(item.getId(), 60)).reversed())
        .toList();

    int pairCount = Math.min(3, Math.min(tops.size(), bottoms.size()));
    List<OutfitRecommendationResponse> outfits = new ArrayList<>();
    for (int i = 0; i < pairCount; i++) {
      Clothing top = tops.get(i);
      Clothing bottom = bottoms.get(i);
      int score = Math.max(scores.getOrDefault(top.getId(), 60), scores.getOrDefault(bottom.getId(), 60));
      outfits.add(new OutfitRecommendationResponse(
          i + 1,
          top.getId(),
          bottom.getId(),
          score,
          "Rule-based recommendation by style overlap and scene compatibility",
          null,
          null
      ));
    }
    return new RecommendationDecision(outfits, null);
  }

  public Map<Long, Integer> buildBaseScores(RecommendationRequest request) {
    Set<String> memberTags = tags(request.member().getStyleTags());
    String scene = request.scene() == null ? "" : request.scene().toLowerCase(Locale.ROOT);

    Map<Long, Integer> result = new HashMap<>();
    for (Clothing clothing : request.candidates()) {
      Set<String> clothingTags = tags(clothing.getStyleTags());
      int overlap = 0;
      for (String tag : clothingTags) {
        if (memberTags.contains(tag)) {
          overlap++;
        }
      }

      int score = 55 + overlap * 12 + ThreadLocalRandom.current().nextInt(0, 12);
      if (!scene.isBlank() && clothingTags.stream().anyMatch(scene::contains)) {
        score += 8;
      }
      result.put(clothing.getId(), Math.max(0, Math.min(100, score)));
    }
    return result;
  }

  private Set<String> tags(String commaText) {
    if (commaText == null || commaText.isBlank()) {
      return Set.of();
    }
    String[] arr = commaText.split(",");
    Set<String> result = new HashSet<>();
    for (String item : arr) {
      String normalized = item.trim().toLowerCase(Locale.ROOT);
      if (!normalized.isBlank()) {
        result.add(normalized);
      }
    }
    return result;
  }
}
