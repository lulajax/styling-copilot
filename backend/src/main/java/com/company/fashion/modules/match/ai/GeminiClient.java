package com.company.fashion.modules.match.ai;

import com.company.fashion.modules.clothing.entity.Clothing;
import com.company.fashion.modules.match.dto.OutfitPreviewResponse;
import com.company.fashion.modules.match.entity.MatchRecord;
import com.company.fashion.modules.member.entity.Member;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GeminiClient extends AbstractLangChainAiClientSupport {

  private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

  @Value("${app.ai.enabled:false}")
  private boolean aiEnabled;

  @Value("${app.ai.gemini.base-url:https://generativelanguage.googleapis.com}")
  private String aiBaseUrl;

  @Value("${app.ai.gemini.model:gemini-2.5-flash}")
  private String aiModel;

  @Value("${app.ai.gemini.api-key:}")
  private String apiKey;

  @Value("${app.ai.gemini.connect-timeout-ms:2000}")
  private int connectTimeoutMs;

  @Value("${app.ai.gemini.read-timeout-ms:8000}")
  private int readTimeoutMs;

  @Value("${app.ai.gemini.proxy.host:}")
  private String proxyHost;

  @Value("${app.ai.gemini.proxy.port:0}")
  private int proxyPort;

  public GeminiClient(PromptBuilder promptBuilder) {
    super(promptBuilder);
  }

  public List<AiClientRouter.AiOutfitSuggestion> suggest(
      Member member,
      List<Clothing> candidates,
      List<MatchRecord> history,
      String scene,
      AiLanguage language
  ) {
    assertSuggestionReady();

    try {
      ChatModel model = buildChatModel();
      String prompt = buildSuggestionPrompt(member, candidates, history, scene, language);
      List<ChatMessage> messages = List.of(UserMessage.from(prompt));
      ChatResponse response = chatWithStructuredFallback(
          model,
          messages,
          512,
          suggestionsType(),
          "Gemini",
          "suggestion",
          log
      );
      List<AbstractLangChainAiClientSupport.AiSuggestionPayload> payloads =
          parseSuggestionPayload(response, "Gemini");
      List<AiClientRouter.AiOutfitSuggestion> normalized = normalizeSuggestions(payloads, candidates);
      if (normalized.isEmpty()) {
        throw new IllegalStateException("Gemini suggestion failed: response contains no valid outfit recommendations");
      }
      return normalized;
    } catch (IllegalStateException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new IllegalStateException("Gemini suggestion failed: " + ex.getMessage(), ex);
    }
  }

  public OutfitPreviewResponse generateOutfitPreview(
      Member member,
      List<Clothing> selected,
      String scene,
      AiLanguage language
  ) {
    assertPreviewReady(selected);

    try {
      ChatModel model = buildChatModel();
      List<ChatMessage> previewMessages = List.of(buildPreviewUserMessage(member, selected, scene, language));
      ChatResponse response = chatWithStructuredFallback(
          model,
          previewMessages,
          800,
          previewType(),
          "Gemini",
          "preview generation",
          log
      );
      AbstractLangChainAiClientSupport.AiPreviewPayload payload = parsePreviewPayload(response, "Gemini");
      return normalizePreview(payload, "Gemini");
    } catch (IllegalStateException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new IllegalStateException("Gemini preview generation failed: " + ex.getMessage(), ex);
    }
  }

  private void assertSuggestionReady() {
    if (!aiEnabled) {
      throw new IllegalStateException("Gemini suggestion failed: AI is disabled");
    }
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException("Gemini suggestion failed: API key is missing");
    }
  }

  private void assertPreviewReady(List<Clothing> selected) {
    if (!aiEnabled) {
      throw new IllegalStateException("Gemini preview generation failed: AI is disabled");
    }
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException("Gemini preview generation failed: API key is missing");
    }
    if (selected == null || selected.isEmpty()) {
      throw new IllegalStateException("Gemini preview generation failed: no selected clothing");
    }
  }

  private ChatModel buildChatModel() {
    GoogleAiGeminiChatModel.GoogleAiGeminiChatModelBuilder builder = GoogleAiGeminiChatModel.builder()
        .httpClientBuilder(buildHttpClientBuilder(
            "Gemini",
            proxyHost,
            proxyPort,
            connectTimeoutMs,
            readTimeoutMs,
            log
        ))
        .apiKey(apiKey)
        .modelName(aiModel)
        .timeout(Duration.ofMillis(Math.max(1000, readTimeoutMs)))
        .temperature(0.2)
        .maxRetries(0)
        .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA);

    if (aiBaseUrl != null && !aiBaseUrl.isBlank()) {
      builder.baseUrl(aiBaseUrl.trim());
    }
    return builder.build();
  }

  private UserMessage buildPreviewUserMessage(Member member, List<Clothing> selected, String scene, AiLanguage language) {
    String prompt = buildPreviewPrompt(member, selected, scene, language);
    List<Content> contents = new ArrayList<>();
    contents.add(TextContent.from(prompt));
    contents.add(TextContent.from("Reference image [member_photo]: keep identity and body shape."));
    contents.add(toImageContent(
        member.getPhotoUrl(),
        true,
        connectTimeoutMs,
        readTimeoutMs,
        "Gemini preview generation failed"
    ));

    for (Clothing clothing : selected) {
      contents.add(TextContent.from(
          "Reference image [clothing] id=%s, type=%s, name=%s. Preserve garment details."
              .formatted(safe(clothing.getId()), safe(clothing.getClothingType()), safe(clothing.getName()))
      ));
      contents.add(toImageContent(
          clothing.getImageUrl(),
          true,
          connectTimeoutMs,
          readTimeoutMs,
          "Gemini preview generation failed"
      ));
    }

    return UserMessage.from(contents);
  }
}
