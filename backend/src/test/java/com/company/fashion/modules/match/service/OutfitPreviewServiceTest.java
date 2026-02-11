package com.company.fashion.modules.match.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.company.fashion.modules.clothing.entity.Clothing;
import com.company.fashion.modules.match.ai.AiLanguage;
import com.company.fashion.modules.match.ai.AiClientRouter;
import com.company.fashion.modules.match.dto.OutfitPreviewResponse;
import com.company.fashion.modules.member.entity.Member;
import java.util.List;
import org.junit.jupiter.api.Test;

class OutfitPreviewServiceTest {

  @Test
  void shouldDegradeWhenMemberPhotoMissing() {
    AiClientRouter router = mock(AiClientRouter.class);
    OutfitPreviewService service = new OutfitPreviewService(router);
    Member member = new Member();
    member.setPhotoUrl(null);
    Clothing clothing = new Clothing();
    clothing.setImageUrl("https://img.test/cloth.jpg");

    OutfitPreviewService.OutfitPreviewDecision decision = service.generate(member, List.of(clothing), "daily-live", AiLanguage.EN);

    assertThat(decision.preview()).isNull();
    assertThat(decision.warning()).isEqualTo(OutfitPreviewService.PREVIEW_DEGRADED_WARNING);
    verifyNoInteractions(router);
  }

  @Test
  void shouldDegradeWhenClothingImageMissing() {
    AiClientRouter router = mock(AiClientRouter.class);
    OutfitPreviewService service = new OutfitPreviewService(router);
    Member member = new Member();
    member.setPhotoUrl("https://img.test/member.jpg");
    Clothing clothing = new Clothing();
    clothing.setImageUrl(null);

    OutfitPreviewService.OutfitPreviewDecision decision = service.generate(member, List.of(clothing), "daily-live", AiLanguage.EN);

    assertThat(decision.preview()).isNull();
    assertThat(decision.warning()).isEqualTo(OutfitPreviewService.PREVIEW_DEGRADED_WARNING);
    verifyNoInteractions(router);
  }

  @Test
  void shouldReturnPreviewWhenImagesReady() {
    AiClientRouter router = mock(AiClientRouter.class);
    OutfitPreviewService service = new OutfitPreviewService(router);
    Member member = new Member();
    member.setPhotoUrl("https://img.test/member.jpg");
    Clothing clothing = new Clothing();
    clothing.setImageUrl("https://img.test/cloth.jpg");

    when(router.generateOutfitPreview(any(), anyList(), anyString(), any(AiLanguage.class)))
        .thenReturn(new OutfitPreviewResponse("title", "desc", "image prompt"));

    OutfitPreviewService.OutfitPreviewDecision decision = service.generate(member, List.of(clothing), "daily-live", AiLanguage.EN);

    assertThat(decision.warning()).isNull();
    assertThat(decision.preview()).isNotNull();
    assertThat(decision.preview().imagePrompt()).isEqualTo("image prompt");
  }
}
