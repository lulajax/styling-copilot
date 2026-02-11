package com.company.fashion.modules.match.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.company.fashion.modules.clothing.entity.Clothing;
import com.company.fashion.modules.clothing.entity.ClothingType;
import com.company.fashion.modules.match.ai.AiLanguage;
import com.company.fashion.modules.match.ai.AiClientRouter;
import com.company.fashion.modules.member.entity.Member;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class HybridRecommendationStrategyTest {

  @Test
  void shouldReturnTopAndBottomWhenBothTypesAvailable() {
    AiClientRouter aiClientRouter = mock(AiClientRouter.class);
    HybridRecommendationStrategy strategy = new HybridRecommendationStrategy(aiClientRouter);

    Clothing top = clothing(1L, ClothingType.TOP);
    Clothing bottom = clothing(2L, ClothingType.BOTTOM);
    Clothing top2 = clothing(3L, ClothingType.TOP);
    Clothing bottom2 = clothing(4L, ClothingType.BOTTOM);
    Clothing top3 = clothing(5L, ClothingType.TOP);
    Clothing bottom3 = clothing(6L, ClothingType.BOTTOM);
    Clothing top4 = clothing(7L, ClothingType.TOP);
    Clothing bottom4 = clothing(8L, ClothingType.BOTTOM);

    List<AiClientRouter.AiOutfitSuggestion> aiResult = List.of(
        new AiClientRouter.AiOutfitSuggestion(1L, 2L, 98, "pair-1"),
        new AiClientRouter.AiOutfitSuggestion(3L, 4L, 96, "pair-2"),
        new AiClientRouter.AiOutfitSuggestion(5L, 6L, 94, "pair-3"),
        new AiClientRouter.AiOutfitSuggestion(7L, 8L, 92, "pair-4")
    );
    when(aiClientRouter.suggest(any(), anyList(), anyList(), anyString(), any(AiLanguage.class))).thenReturn(aiResult);

    RecommendationStrategy.RecommendationDecision decision = strategy.recommend(
        new RecommendationRequest(
            new Member(),
            List.of(top, bottom, top2, bottom2, top3, bottom3, top4, bottom4),
            List.of(),
            "daily-live",
            AiLanguage.EN
        )
    );

    assertThat(decision.warning()).isNull();
    assertThat(decision.outfits()).hasSize(4);
    assertThat(decision.outfits().getFirst().topClothingId()).isEqualTo(1L);
    assertThat(decision.outfits().getFirst().bottomClothingId()).isEqualTo(2L);
    assertThat(strategy.strategyName()).isEqualTo("AI_ONLY");
  }

  @Test
  void shouldReturnAvailableOutfitsWithoutDegradeWarning() {
    AiClientRouter aiClientRouter = mock(AiClientRouter.class);
    HybridRecommendationStrategy strategy = new HybridRecommendationStrategy(aiClientRouter);

    Clothing top1 = clothing(1L, ClothingType.TOP);
    Clothing bottom1 = clothing(2L, ClothingType.BOTTOM);
    Clothing top2 = clothing(3L, ClothingType.TOP);
    Clothing bottom2 = clothing(4L, ClothingType.BOTTOM);

    List<AiClientRouter.AiOutfitSuggestion> aiResult = List.of(
        new AiClientRouter.AiOutfitSuggestion(1L, 2L, 95, "look-1"),
        new AiClientRouter.AiOutfitSuggestion(3L, 4L, 90, "look-2")
    );
    when(aiClientRouter.suggest(any(), anyList(), anyList(), anyString(), any(AiLanguage.class))).thenReturn(aiResult);

    RecommendationStrategy.RecommendationDecision decision = strategy.recommend(
        new RecommendationRequest(new Member(), List.of(top1, bottom1, top2, bottom2), List.of(), "daily-live", AiLanguage.EN)
    );

    assertThat(decision.outfits()).hasSize(2);
    assertThat(decision.warning()).isNull();
  }

  @Test
  void shouldThrowWhenAiReturnsNoUsableRecommendation() {
    AiClientRouter aiClientRouter = mock(AiClientRouter.class);
    HybridRecommendationStrategy strategy = new HybridRecommendationStrategy(aiClientRouter);

    when(aiClientRouter.suggest(any(), anyList(), anyList(), anyString(), any(AiLanguage.class))).thenReturn(List.of());

    assertThatThrownBy(() -> strategy.recommend(
        new RecommendationRequest(new Member(), List.of(clothing(1L, ClothingType.TOP)), List.of(), "daily-live", AiLanguage.EN)
    )).isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("AI outfit recommendation unavailable or invalid");
  }

  @Test
  void shouldSupportColdAndNonColdStart() {
    HybridRecommendationStrategy strategy = new HybridRecommendationStrategy(mock(AiClientRouter.class));
    assertThat(strategy.supports(true)).isTrue();
    assertThat(strategy.supports(false)).isTrue();
  }

  private Clothing clothing(Long id, ClothingType type) {
    Clothing clothing = new Clothing();
    ReflectionTestUtils.setField(clothing, "id", id);
    clothing.setName("c" + id);
    clothing.setClothingType(type);
    return clothing;
  }
}
