package com.company.fashion.modules.match.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.fashion.modules.clothing.entity.Clothing;
import com.company.fashion.modules.clothing.entity.ClothingType;
import com.company.fashion.modules.member.entity.Member;
import com.company.fashion.modules.member.service.BodyProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PromptBuilderTest {

  private final PromptBuilder promptBuilder = new PromptBuilder(new BodyProfileService(new ObjectMapper()));

  @Test
  void shouldIncludeLanguageConstraintInPrompt() {
    Member member = new Member();
    member.setName("MemberA");
    member.setStyleTags("kpop_sweet_cool");

    Clothing top = new Clothing();
    ReflectionTestUtils.setField(top, "id", 1L);
    top.setName("Top A");
    top.setClothingType(ClothingType.TOP);
    top.setStyleTags("kpop_sweet_cool");

    String zhPrompt = promptBuilder.buildPrompt(member, List.of(top), List.of(), "daily-live", AiLanguage.ZH);
    String enPrompt = promptBuilder.buildPrompt(member, List.of(top), List.of(), "daily-live", AiLanguage.EN);
    String koPrompt = promptBuilder.buildPrompt(member, List.of(top), List.of(), "daily-live", AiLanguage.KO);

    assertThat(zhPrompt).contains("must be written in Chinese");
    assertThat(enPrompt).contains("must be written in English");
    assertThat(koPrompt).contains("must be written in Korean");
  }
}
