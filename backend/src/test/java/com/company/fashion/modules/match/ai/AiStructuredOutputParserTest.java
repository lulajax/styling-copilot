package com.company.fashion.modules.match.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AiStructuredOutputParserTest {

    private static final Logger log = LoggerFactory.getLogger(AiStructuredOutputParserTest.class);

  private final AbstractLangChainAiClientSupport.StructuredOutputParser parser =
      new AbstractLangChainAiClientSupport.StructuredOutputParser();

  @Test
  void shouldParseSuggestionsToTypedPayload() {
    String text = """
        [
          {"topClothingId":1,"bottomClothingId":2,"score":88,"reason":"good fit"}
        ]
        """;
    ChatResponse response = ChatResponse.builder().aiMessage(AiMessage.from(text)).build();

    List<AbstractLangChainAiClientSupport.AiSuggestionPayload> suggestions =
        parser.parseSuggestions(response, "OpenAI", log);

    assertThat(suggestions).hasSize(1);
    assertThat(suggestions.getFirst().getTopClothingId()).isEqualTo(1L);
    assertThat(suggestions.getFirst().getBottomClothingId()).isEqualTo(2L);
    assertThat(suggestions.getFirst().getScore()).isEqualTo(88);
    assertThat(suggestions.getFirst().getReason()).isEqualTo("good fit");
  }

  @Test
  void shouldParsePreviewToTypedPayload() {
    String text = """
        {
          "title":"Look #1",
          "outfitDescription":"Simple and clean.",
          "imagePrompt":"A realistic try-on"
        }
        """;
    ChatResponse response = ChatResponse.builder().aiMessage(AiMessage.from(text)).build();

    AbstractLangChainAiClientSupport.AiPreviewPayload preview = parser.parsePreview(response, "Gemini", log);

    assertThat(preview.getTitle()).isEqualTo("Look #1");
    assertThat(preview.getOutfitDescription()).isEqualTo("Simple and clean.");
    assertThat(preview.getImagePrompt()).isEqualTo("A realistic try-on");
  }

  @Test
  void shouldThrowWhenStructuredOutputIsInvalid() {
    ChatResponse response = ChatResponse.builder().aiMessage(AiMessage.from("not json")).build();

    assertThatThrownBy(() -> parser.parseSuggestions(response, "OpenAI", log))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("invalid structured output");
  }

  @Test
  void shouldBuildJsonSchemaResponseFormat() {
    ResponseFormat format = parser.buildResponseFormatFor(parser.suggestionsType(), "OpenAI", "suggestion");

    assertThat(format.type()).isEqualTo(ResponseFormatType.JSON);
    assertThat(format.jsonSchema()).isNotNull();
  }
}
