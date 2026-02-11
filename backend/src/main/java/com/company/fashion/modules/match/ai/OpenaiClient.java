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
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OpenaiClient extends AbstractLangChainAiClientSupport {

  private static final Logger log = LoggerFactory.getLogger(OpenaiClient.class);

  @Value("${app.ai.enabled:false}")
  private boolean aiEnabled;

  @Value("${app.ai.openai.base-url:https://api.openai.com}")
  private String openaiBaseUrl;

  @Value("${app.ai.openai.model:gpt-4o-mini}")
  private String openaiModel;

  @Value("${app.ai.openai.api-key:}")
  private String openaiApiKey;

  @Value("${app.ai.openai.connect-timeout-ms:30000}")
  private int connectTimeoutMs;

  @Value("${app.ai.openai.read-timeout-ms:120000}")
  private int readTimeoutMs;

  @Value("${app.ai.openai.proxy.host:}")
  private String proxyHost;

  @Value("${app.ai.openai.proxy.port:0}")
  private int proxyPort;

  @Value("${app.ai.openai.use-data-url-images:true}")
  private boolean useDataUrlImages;

  @Value("${app.ai.openai.log-response:true}")
  private boolean logResponse;

  @Value("${app.ai.openai.log-response-max-chars:1200}")
  private int logResponseMaxChars;

  @Value("${app.ai.openai.preview-max-retries:2}")
  private int previewMaxRetries;

  @Value("${app.ai.openai.preview-retry-backoff-ms:600}")
  private long previewRetryBackoffMs;

  public OpenaiClient(PromptBuilder promptBuilder) {
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
      ChatModel model = buildChatModel(readTimeoutMs);
      String prompt = buildSuggestionPrompt(member, candidates, history, scene, language);
      List<ChatMessage> messages = List.of(UserMessage.from(prompt));
      ChatResponse response = chatWithStructuredFallback(
          model,
          messages,
          512,
          suggestionsType(),
          "OpenAI",
          "suggestion",
          log
      );
      logResponsePreview(log, "OpenAI", "suggestion", response, logResponse, logResponseMaxChars);

      List<AbstractLangChainAiClientSupport.AiSuggestionPayload> payloads =
          parseSuggestionPayload(response, "OpenAI");
      List<AiClientRouter.AiOutfitSuggestion> normalized = normalizeSuggestions(payloads, candidates);
      if (normalized.isEmpty()) {
        throw new IllegalStateException("OpenAI suggestion failed: response contains no valid outfit recommendations");
      }
      return normalized;
    } catch (IllegalStateException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new IllegalStateException("OpenAI suggestion failed: " + ex.getMessage(), ex);
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
      ChatModel model = buildChatModel(readTimeoutMs);
      List<ChatMessage> previewMessages = List.of(buildPreviewUserMessage(member, selected, scene, language));
      int maxAttempts = Math.max(1, previewMaxRetries + 1);

      Throwable lastError = null;
      for (int attempt = 1; attempt <= maxAttempts; attempt++) {
        try {
          ChatResponse response = chatWithStructuredFallback(
              model,
              previewMessages,
              800,
              previewType(),
              "OpenAI",
              "preview generation",
              log
          );
          logResponsePreview(log, "OpenAI", "preview generation", response, logResponse, logResponseMaxChars);

          AbstractLangChainAiClientSupport.AiPreviewPayload payload =
              parsePreviewPayload(response, "OpenAI");
          return normalizePreview(payload, "OpenAI");
        } catch (Throwable ex) {
          lastError = ex;
          if (!isRetryablePreviewError(ex) || attempt >= maxAttempts) {
            throw ex;
          }
          long backoff = Math.max(0L, previewRetryBackoffMs) * attempt;
          log.warn(
              "OpenAI preview timed out, retrying attempt {}/{} after {} ms",
              attempt + 1,
              maxAttempts,
              backoff
          );
          sleepQuietly(backoff, "OpenAI preview generation failed");
        }
      }

      throw new IllegalStateException("OpenAI preview generation failed: unknown retry failure", lastError);
    } catch (IllegalStateException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new IllegalStateException("OpenAI preview generation failed: " + ex.getMessage(), ex);
    }
  }

  private void assertSuggestionReady() {
    if (!aiEnabled) {
      throw new IllegalStateException("OpenAI suggestion failed: AI is disabled");
    }
    if (openaiApiKey == null || openaiApiKey.isBlank()) {
      throw new IllegalStateException("OpenAI suggestion failed: API key is missing");
    }
  }

  private void assertPreviewReady(List<Clothing> selected) {
    if (!aiEnabled) {
      throw new IllegalStateException("OpenAI preview generation failed: AI is disabled");
    }
    if (openaiApiKey == null || openaiApiKey.isBlank()) {
      throw new IllegalStateException("OpenAI preview generation failed: API key is missing");
    }
    if (selected == null || selected.isEmpty()) {
      throw new IllegalStateException("OpenAI preview generation failed: no selected clothing");
    }
  }

  private ChatModel buildChatModel(int timeoutMs) {
    HttpClientBuilder httpClientBuilder = buildHttpClientBuilder(
        "OpenAI",
        proxyHost,
        proxyPort,
        connectTimeoutMs,
        timeoutMs,
        log
    );
    return OpenAiChatModel.builder()
        .httpClientBuilder(httpClientBuilder)
        .apiKey(openaiApiKey)
        .baseUrl(normalizeOpenAiBaseUrl(openaiBaseUrl))
        .modelName(openaiModel)
        .timeout(Duration.ofMillis(Math.max(1000, timeoutMs)))
        .temperature(0.2)
        .maxRetries(0)
        .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
        .strictJsonSchema(true)
        .logRequests(logResponse)
        .logResponses(logResponse)
        .build();
  }

  private String normalizeOpenAiBaseUrl(String raw) {
    if (raw == null || raw.isBlank()) {
      return "https://api.openai.com/v1";
    }
    String value = raw.trim();
    if (value.endsWith("/")) {
      value = value.substring(0, value.length() - 1);
    }
    if (value.endsWith("/v1")) {
      return value;
    }
    return value + "/v1";
  }

  private UserMessage buildPreviewUserMessage(Member member, List<Clothing> selected, String scene, AiLanguage language) {
    String prompt = buildPreviewPrompt(member, selected, scene, language);
    List<Content> contents = new ArrayList<>();
    contents.add(TextContent.from(prompt));
    contents.add(TextContent.from("Reference image [member_photo]: keep identity and body shape."));
    contents.add(toImageContent(
        member.getPhotoUrl(),
        useDataUrlImages,
        connectTimeoutMs,
        readTimeoutMs,
        "OpenAI preview generation failed"
    ));

    for (Clothing clothing : selected) {
      contents.add(TextContent.from(
          "Reference image [clothing] id=%s, type=%s, name=%s. Preserve garment details."
              .formatted(safe(clothing.getId()), safe(clothing.getClothingType()), safe(clothing.getName()))
      ));
      contents.add(toImageContent(
          clothing.getImageUrl(),
          useDataUrlImages,
          connectTimeoutMs,
          readTimeoutMs,
          "OpenAI preview generation failed"
      ));
    }

    return UserMessage.from(contents);
  }
}
