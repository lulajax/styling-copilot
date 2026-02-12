package com.company.fashion.modules.match.ai;

import com.company.fashion.modules.clothing.entity.Clothing;
import com.company.fashion.modules.clothing.entity.ClothingType;
import com.company.fashion.modules.match.dto.OutfitPreviewResponse;
import com.company.fashion.modules.member.entity.Member;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.output.JsonSchemas;
import dev.langchain4j.service.output.ServiceOutputParser;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URL;
import java.net.URLConnection;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractLangChainAiClientSupport {

  private final PromptBuilder promptBuilder;
  private final StructuredOutputParser structuredOutputParser;
  private final Set<String> proxyLogMarkers = ConcurrentHashMap.newKeySet();

  protected AbstractLangChainAiClientSupport(PromptBuilder promptBuilder) {
    this.promptBuilder = promptBuilder;
    this.structuredOutputParser = new StructuredOutputParser();
  }

  protected String buildSuggestionPrompt(
      Member member,
      List<Clothing> candidates,
      List<com.company.fashion.modules.match.entity.MatchRecord> history,
      String scene,
      AiLanguage language
  ) {
    return promptBuilder.buildPrompt(member, candidates, history, scene, language);
  }

  protected ChatResponse chatWithStructuredFallback(
      ChatModel model,
      List<dev.langchain4j.data.message.ChatMessage> messages,
      Type responseType,
      String provider,
      String operation,
      Logger logger
  ) {
    ResponseFormat responseFormat = structuredOutputParser.buildResponseFormatFor(responseType, provider, operation);
    logger.info(
        "{} {} request: format={}, schema={}, maxOutputTokens=unlimited, messageCount={}",
        provider,
        operation,
        responseFormat.type(),
        responseFormat.jsonSchema() == null ? "-" : responseFormat.jsonSchema().name(),
        messages == null ? 0 : messages.size()
    );

    ChatRequest schemaRequest = ChatRequest.builder()
        .messages(messages)
        .responseFormat(responseFormat)
        .build();

    try {
      ChatResponse schemaResponse = model.chat(schemaRequest);
      logChatResponseMeta(logger, provider, operation, schemaResponse, "schema");
      return schemaResponse;
    } catch (RuntimeException ex) {
      if (!isJsonSchemaUnsupported(ex)) {
        throw ex;
      }
      logger.warn("{} {} fallback to JSON mode: {}", provider, operation, ex.getMessage());
      ChatRequest jsonRequest = ChatRequest.builder()
          .messages(messages)
          .responseFormat(structuredOutputParser.jsonOnlyResponseFormat())
          .build();
      ChatResponse jsonResponse = model.chat(jsonRequest);
      logChatResponseMeta(logger, provider, operation, jsonResponse, "json");
      return jsonResponse;
    }
  }

  protected Type suggestionsType() {
    return structuredOutputParser.suggestionsType();
  }

  protected Type previewType() {
    return structuredOutputParser.previewType();
  }

  protected List<AiSuggestionPayload> parseSuggestionPayload(ChatResponse response, String provider, Logger logger) {
    return structuredOutputParser.parseSuggestions(response, provider, logger);
  }

  protected AiPreviewPayload parsePreviewPayload(ChatResponse response, String provider, Logger logger) {
    return structuredOutputParser.parsePreview(response, provider, logger);
  }

  protected void logResponsePreview(
      Logger logger,
      String provider,
      String operation,
      ChatResponse response,
      boolean enabled,
      int maxChars
  ) {
    if (!enabled) {
      return;
    }
    String text = response == null || response.aiMessage() == null ? null : response.aiMessage().text();
    if (text == null) {
      logger.info("{} {} response: <empty>", provider, operation);
      return;
    }
    String normalized = text.replace("\r", "\\r").replace("\n", "\\n");
    if (normalized.length() > maxChars) {
      normalized = normalized.substring(0, maxChars) + "...(truncated)";
    }
    logger.info("{} {} response preview={}", provider, operation, normalized);
  }

  protected void logChatResponseMeta(
      Logger logger,
      String provider,
      String operation,
      ChatResponse response,
      String mode
  ) {
    if (response == null) {
      logger.warn("{} {} {} response is null", provider, operation, mode);
      return;
    }
    String text = response.aiMessage() == null ? null : response.aiMessage().text();
    logger.info(
        "{} {} {} response meta: model={}, finishReason={}, tokenUsage={}, textLength={}",
        provider,
        operation,
        mode,
        response.modelName(),
        response.finishReason(),
        response.tokenUsage(),
        text == null ? 0 : text.length()
    );
  }

  protected List<AiClientRouter.AiOutfitSuggestion> normalizeSuggestions(
      List<AiSuggestionPayload> parsed,
      List<Clothing> candidates
  ) {
    if (parsed == null || parsed.isEmpty()) {
      return List.of();
    }

    Map<Long, Clothing> candidateMap = new HashMap<>();
    for (Clothing candidate : candidates) {
      candidateMap.put(candidate.getId(), candidate);
    }

    List<AiClientRouter.AiOutfitSuggestion> parsedSuggestions = new ArrayList<>();
    for (AiSuggestionPayload item : parsed) {
      Long topClothingId = item.getTopClothingId();
      Long bottomClothingId = item.getBottomClothingId();
      Integer score = item.getScore();
      String reason = safe(item.getReason());
      if (reason.isBlank()) {
        reason = "AI outfit recommendation";
      }

      if (topClothingId == null || bottomClothingId == null || score == null || topClothingId.equals(bottomClothingId)) {
        continue;
      }
      parsedSuggestions.add(new AiClientRouter.AiOutfitSuggestion(
          topClothingId,
          bottomClothingId,
          clampScore(score),
          reason
      ));
    }

    parsedSuggestions.sort(Comparator.comparingInt(AiClientRouter.AiOutfitSuggestion::score).reversed());

    List<AiClientRouter.AiOutfitSuggestion> ranked = new ArrayList<>();
    for (AiClientRouter.AiOutfitSuggestion item : parsedSuggestions) {
      Long topClothingId = item.topClothingId();
      Long bottomClothingId = item.bottomClothingId();

      Clothing topCandidate = candidateMap.get(topClothingId);
      Clothing bottomCandidate = candidateMap.get(bottomClothingId);
      if (topCandidate == null || bottomCandidate == null) {
        continue;
      }
      if (topCandidate.getClothingType() == ClothingType.BOTTOM && bottomCandidate.getClothingType() == ClothingType.TOP) {
        Clothing temp = topCandidate;
        topCandidate = bottomCandidate;
        bottomCandidate = temp;
        Long tempId = topClothingId;
        topClothingId = bottomClothingId;
        bottomClothingId = tempId;
      }
      if (topCandidate.getClothingType() != ClothingType.TOP || bottomCandidate.getClothingType() != ClothingType.BOTTOM) {
        continue;
      }

      ranked.add(new AiClientRouter.AiOutfitSuggestion(
          topClothingId,
          bottomClothingId,
          item.score(),
          safe(item.reason())
      ));
    }
    return ranked;
  }

  protected OutfitPreviewResponse normalizePreview(AiPreviewPayload parsed, String provider) {
    if (parsed == null) {
      throw new IllegalStateException(provider + " preview generation failed: response missing preview payload");
    }

    String title = safe(parsed.getTitle());
    String outfitDescription = safe(parsed.getOutfitDescription());
    String imagePrompt = safe(parsed.getImagePrompt());

    if (title.isBlank() || outfitDescription.isBlank() || imagePrompt.isBlank()) {
      throw new IllegalStateException(provider + " preview generation failed: response missing required preview fields");
    }
    return new OutfitPreviewResponse(title, outfitDescription, imagePrompt);
  }

  protected String buildPreviewPrompt(Member member, List<Clothing> selected, String scene, AiLanguage language) {
    return promptBuilder.buildPreviewPrompt(member, selected, scene, language);
  }

  protected HttpClientBuilder buildHttpClientBuilder(
      String provider,
      String proxyHost,
      int proxyPort,
      int connectTimeoutMs,
      int readTimeoutMs,
      Logger logger
  ) {
    JdkHttpClientBuilder httpClientBuilder = new JdkHttpClientBuilder()
        .httpClientBuilder(java.net.http.HttpClient.newBuilder())
        .connectTimeout(Duration.ofMillis(Math.max(1000, connectTimeoutMs)))
        .readTimeout(Duration.ofMillis(Math.max(1000, readTimeoutMs)));

    String normalizedProxyHost = proxyHost == null ? "" : proxyHost.trim();
    if (normalizedProxyHost.isEmpty()) {
      logProxyStatusOnce(provider + "|direct", () -> logger.debug("{} proxy disabled, using direct connection", provider));
      return httpClientBuilder;
    }
    if (proxyPort <= 0) {
      throw new IllegalStateException(
          provider + " proxy configuration invalid: proxy.port must be > 0 when proxy.host is set"
      );
    }

    httpClientBuilder.httpClientBuilder()
        .proxy(ProxySelector.of(new InetSocketAddress(normalizedProxyHost, proxyPort)));
    logProxyStatusOnce(
        provider + "|proxy|" + normalizedProxyHost + ":" + proxyPort,
        () -> logger.info("{} proxy enabled: {}:{}", provider, normalizedProxyHost, proxyPort)
    );
    return httpClientBuilder;
  }

  protected ImageContent toImageContent(
      String imageUrl,
      boolean asDataUrl,
      int connectTimeoutMs,
      int readTimeoutMs,
      String providerErrorPrefix
  ) {
    if (imageUrl == null || imageUrl.isBlank()) {
      throw new IllegalStateException(providerErrorPrefix + ": image URL is missing");
    }
    if (!asDataUrl) {
      return ImageContent.from(imageUrl, ImageContent.DetailLevel.HIGH);
    }

    ImageAttachment attachment = downloadImageAttachment(imageUrl, connectTimeoutMs, readTimeoutMs, providerErrorPrefix);
    String base64 = Base64.getEncoder().encodeToString(attachment.bytes());
    Image image = Image.builder()
        .base64Data(base64)
        .mimeType(attachment.mimeType())
        .build();
    return ImageContent.from(image, ImageContent.DetailLevel.HIGH);
  }

  protected String safe(Object value) {
    return value == null ? "" : String.valueOf(value);
  }

  protected boolean isRetryablePreviewError(Throwable ex) {
    Throwable cursor = ex;
    while (cursor != null) {
      if (cursor instanceof java.net.SocketTimeoutException || cursor instanceof java.net.http.HttpTimeoutException) {
        return true;
      }
      if (containsIgnoreCase(cursor.getMessage(), "timed out") || containsIgnoreCase(cursor.getMessage(), "timeout")) {
        return true;
      }
      cursor = cursor.getCause();
    }
    return false;
  }

  protected void sleepQuietly(long millis, String providerErrorPrefix) {
    if (millis <= 0) {
      return;
    }
    try {
      Thread.sleep(millis);
    } catch (InterruptedException interruptedException) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(providerErrorPrefix + ": interrupted during retry backoff", interruptedException);
    }
  }

  private boolean containsIgnoreCase(String text, String needle) {
    if (text == null || needle == null) {
      return false;
    }
    return text.toLowerCase().contains(needle.toLowerCase());
  }

  private void logProxyStatusOnce(String marker, Runnable action) {
    if (proxyLogMarkers.add(marker)) {
      action.run();
    }
  }

  private int clampScore(int score) {
    return Math.max(0, Math.min(100, score));
  }

  private boolean isJsonSchemaUnsupported(Throwable ex) {
    Throwable cursor = ex;
    while (cursor != null) {
      String message = cursor.getMessage();
      if (containsIgnoreCase(message, "json schema")
          || containsIgnoreCase(message, "response_format")
          || containsIgnoreCase(message, "response format")) {
        return true;
      }
      cursor = cursor.getCause();
    }
    return false;
  }

  private ImageAttachment downloadImageAttachment(
      String imageUrl,
      int connectTimeoutMs,
      int readTimeoutMs,
      String providerErrorPrefix
  ) {
    try {
      URL url = new URL(imageUrl);
      URLConnection connection = url.openConnection();
      connection.setConnectTimeout(connectTimeoutMs);
      connection.setReadTimeout(readTimeoutMs);
      String mimeType = normalizeMimeType(connection.getContentType(), imageUrl);
      try (InputStream inputStream = connection.getInputStream()) {
        byte[] bytes = inputStream.readAllBytes();
        if (bytes.length == 0) {
          throw new IllegalStateException(providerErrorPrefix + ": empty image content");
        }
        return new ImageAttachment(bytes, mimeType);
      }
    } catch (IllegalStateException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new IllegalStateException(providerErrorPrefix + ": unable to load image: " + ex.getMessage(), ex);
    }
  }

  private String normalizeMimeType(String contentType, String imageUrl) {
    String raw = contentType == null ? "" : contentType.trim().toLowerCase();
    if (raw.startsWith("image/")) {
      int separatorIdx = raw.indexOf(';');
      return separatorIdx > 0 ? raw.substring(0, separatorIdx) : raw;
    }

    String lowerUrl = imageUrl.toLowerCase();
    if (lowerUrl.endsWith(".png")) {
      return "image/png";
    }
    if (lowerUrl.endsWith(".webp")) {
      return "image/webp";
    }
    if (lowerUrl.endsWith(".gif")) {
      return "image/gif";
    }
    return "image/jpeg";
  }

  static final class StructuredOutputParser {

    private static final Type SUGGESTIONS_TYPE = AiSuggestionEnvelope.class;
    private static final Type PREVIEW_TYPE = AiPreviewPayload.class;
    private static final int MAX_SCHEMA_NAME_LENGTH = 64;
    private static final Logger log = LoggerFactory.getLogger(StructuredOutputParser.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final ServiceOutputParser outputParser = new ServiceOutputParser();

    Type suggestionsType() {
      return SUGGESTIONS_TYPE;
    }

    Type previewType() {
      return PREVIEW_TYPE;
    }

    List<AiSuggestionPayload> parseSuggestions(ChatResponse response, String provider, Logger externalLogger) {
      try {
        AiSuggestionEnvelope result = (AiSuggestionEnvelope) outputParser.parse(response, SUGGESTIONS_TYPE);
        if (result == null || result.getOutfits() == null || result.getOutfits().length == 0) {
          return List.of();
        }
        return Arrays.asList(result.getOutfits());
      } catch (Exception ex) {
        try {
          AiSuggestionPayload[] rawArray = parseSuggestionsFromRawArray(response);
          if (rawArray == null || rawArray.length == 0) {
            return List.of();
          }
          return Arrays.asList(rawArray);
        } catch (Exception fallbackEx) {
          fallbackEx.addSuppressed(ex);
          logStructuredParseFailure(provider, "suggestion", response, fallbackEx, externalLogger);
          throw new IllegalStateException(provider + " suggestion failed: invalid structured output: " + fallbackEx.getMessage(), fallbackEx);
        }
      }
    }

    AiPreviewPayload parsePreview(ChatResponse response, String provider, Logger externalLogger) {
      try {
        AiPreviewPayload result = (AiPreviewPayload) outputParser.parse(response, PREVIEW_TYPE);
        if (result == null) {
          throw new IllegalStateException(provider + " preview generation failed: empty structured output");
        }
        return result;
      } catch (IllegalStateException ex) {
        logStructuredParseFailure(provider, "preview", response, ex, externalLogger);
        throw ex;
      } catch (Exception ex) {
        logStructuredParseFailure(provider, "preview", response, ex, externalLogger);
        throw new IllegalStateException(provider + " preview generation failed: invalid structured output: " + ex.getMessage(), ex);
      }
    }

    ResponseFormat buildResponseFormatFor(Type type, String provider, String operation) {
      JsonSchema schema = JsonSchemas.jsonSchemaFrom(type)
          .map(item -> JsonSchema.builder()
              .name(sanitizeSchemaName(item.name(), provider, operation))
              .rootElement(item.rootElement())
              .build())
          .orElse(null);

      if (schema == null) {
        return jsonOnlyResponseFormat();
      }

      return ResponseFormat.builder()
          .type(ResponseFormatType.JSON)
          .jsonSchema(schema)
          .build();
    }

    ResponseFormat jsonOnlyResponseFormat() {
      return ResponseFormat.builder()
          .type(ResponseFormatType.JSON)
          .build();
    }

    private String sanitizeSchemaName(String raw, String provider, String operation) {
      String base = raw;
      if (base == null || base.isBlank()) {
        base = provider + "_" + operation + "_response";
      }
      base = base.replaceAll("[^a-zA-Z0-9_-]", "_");
      base = base.replaceAll("_+", "_");
      if (base.isBlank()) {
        base = "structured_output";
      }
      if (base.length() > MAX_SCHEMA_NAME_LENGTH) {
        base = base.substring(0, MAX_SCHEMA_NAME_LENGTH);
      }
      return base;
    }

    private AiSuggestionPayload[] parseSuggestionsFromRawArray(ChatResponse response) throws Exception {
      String text = response == null || response.aiMessage() == null ? null : response.aiMessage().text();
      if (text == null || text.isBlank()) {
        return new AiSuggestionPayload[0];
      }
      return mapper.readValue(text, AiSuggestionPayload[].class);
    }

    private void logStructuredParseFailure(
        String provider,
        String operation,
        ChatResponse response,
        Throwable ex,
        Logger externalLogger
    ) {
      Logger target = externalLogger == null ? log : externalLogger;
      String text = response == null || response.aiMessage() == null ? null : response.aiMessage().text();
      target.error(
          "{} {} structured parse failed: model={}, finishReason={}, tokenUsage={}, textLength={}, textTail={}",
          provider,
          operation,
          response == null ? "-" : response.modelName(),
          response == null ? "-" : response.finishReason(),
          response == null ? "-" : response.tokenUsage(),
          text == null ? 0 : text.length(),
          tail(text, 320),
          ex
      );
    }

    private String tail(String text, int maxChars) {
      if (text == null || text.isBlank()) {
        return "<empty>";
      }
      String normalized = text.replace("\r", "\\r").replace("\n", "\\n");
      if (normalized.length() <= maxChars) {
        return normalized;
      }
      return "..."+ normalized.substring(normalized.length() - maxChars);
    }
  }

  static final class AiSuggestionPayload {

    private Long topClothingId;
    private Long bottomClothingId;
    private Integer score;
    private String reason;

    public Long getTopClothingId() {
      return topClothingId;
    }

    public void setTopClothingId(Long topClothingId) {
      this.topClothingId = topClothingId;
    }

    public Long getBottomClothingId() {
      return bottomClothingId;
    }

    public void setBottomClothingId(Long bottomClothingId) {
      this.bottomClothingId = bottomClothingId;
    }

    public Integer getScore() {
      return score;
    }

    public void setScore(Integer score) {
      this.score = score;
    }

    public String getReason() {
      return reason;
    }

    public void setReason(String reason) {
      this.reason = reason;
    }
  }

  static final class AiSuggestionEnvelope {

    private AiSuggestionPayload[] outfits;

    public AiSuggestionPayload[] getOutfits() {
      return outfits;
    }

    public void setOutfits(AiSuggestionPayload[] outfits) {
      this.outfits = outfits;
    }
  }

  static final class AiPreviewPayload {

    private String title;
    private String outfitDescription;
    private String imagePrompt;

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public String getOutfitDescription() {
      return outfitDescription;
    }

    public void setOutfitDescription(String outfitDescription) {
      this.outfitDescription = outfitDescription;
    }

    public String getImagePrompt() {
      return imagePrompt;
    }

    public void setImagePrompt(String imagePrompt) {
      this.imagePrompt = imagePrompt;
    }
  }

  private record ImageAttachment(byte[] bytes, String mimeType) {
  }
}
