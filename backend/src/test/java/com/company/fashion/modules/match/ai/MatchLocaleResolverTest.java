package com.company.fashion.modules.match.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MatchLocaleResolverTest {

  private final MatchLocaleResolver resolver = new MatchLocaleResolver();

  @Test
  void shouldResolveSupportedLocales() {
    assertThat(resolver.resolve("zh-CN")).isEqualTo(AiLanguage.ZH);
    assertThat(resolver.resolve("ko-KR,ko;q=0.9")).isEqualTo(AiLanguage.KO);
    assertThat(resolver.resolve("en-US")).isEqualTo(AiLanguage.EN);
  }

  @Test
  void shouldFallbackToEnglish() {
    assertThat(resolver.resolve(null)).isEqualTo(AiLanguage.EN);
    assertThat(resolver.resolve("")).isEqualTo(AiLanguage.EN);
    assertThat(resolver.resolve("fr-FR")).isEqualTo(AiLanguage.EN);
  }
}
