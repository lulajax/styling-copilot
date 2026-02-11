package com.company.fashion.modules.match.ai;

import java.util.Locale;

public enum AiLanguage {
  ZH("zh", "Chinese"),
  EN("en", "English"),
  KO("ko", "Korean");

  private final String code;
  private final String promptLabel;

  AiLanguage(String code, String promptLabel) {
    this.code = code;
    this.promptLabel = promptLabel;
  }

  public String code() {
    return code;
  }

  public String promptLabel() {
    return promptLabel;
  }

  public static AiLanguage fromCode(String raw) {
    if (raw == null || raw.isBlank()) {
      return EN;
    }
    String normalized = raw.trim().toLowerCase(Locale.ROOT);
    if ("zh".equals(normalized)) {
      return ZH;
    }
    if ("ko".equals(normalized)) {
      return KO;
    }
    return EN;
  }
}
