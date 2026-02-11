package com.company.fashion.modules.match.ai;

import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class MatchLocaleResolver {

  public AiLanguage resolve(String acceptLanguage) {
    if (acceptLanguage == null || acceptLanguage.isBlank()) {
      return AiLanguage.EN;
    }
    String firstLanguage = acceptLanguage.split(",")[0].trim().toLowerCase(Locale.ROOT);
    if (firstLanguage.startsWith("zh")) {
      return AiLanguage.ZH;
    }
    if (firstLanguage.startsWith("ko")) {
      return AiLanguage.KO;
    }
    return AiLanguage.EN;
  }
}
