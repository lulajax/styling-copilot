package com.company.fashion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.fashion.modules.clothing.entity.Clothing;
import com.company.fashion.modules.clothing.entity.ClothingType;
import com.company.fashion.modules.match.ai.AiLanguage;
import com.company.fashion.modules.match.ai.AiClientRouter;
import com.company.fashion.modules.match.dto.OutfitPreviewResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class MatchTaskIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private AiClientRouter aiClientRouter;

  @BeforeEach
  void setUpAiMocks() {
    when(aiClientRouter.suggest(any(), anyList(), anyList(), anyString(), any(AiLanguage.class)))
        .thenAnswer(invocation -> {
          @SuppressWarnings("unchecked")
          List<Clothing> candidates = (List<Clothing>) invocation.getArgument(1);
          AiLanguage language = invocation.getArgument(4);
          String reason = switch (language) {
            case ZH -> "ZH_REASON";
            case KO -> "KO_REASON";
            default -> "EN_REASON";
          };
          List<Long> tops = candidates.stream()
              .filter(item -> item.getClothingType() == ClothingType.TOP)
              .map(Clothing::getId)
              .toList();
          List<Long> bottoms = candidates.stream()
              .filter(item -> item.getClothingType() == ClothingType.BOTTOM)
              .map(Clothing::getId)
              .toList();
          int pairs = Math.min(3, Math.min(tops.size(), bottoms.size()));
          List<AiClientRouter.AiOutfitSuggestion> result = new ArrayList<>();
          for (int i = 0; i < pairs; i++) {
            result.add(new AiClientRouter.AiOutfitSuggestion(
                tops.get(i),
                bottoms.get(i),
                95 - i,
                reason
            ));
          }
          return result;
        });
    when(aiClientRouter.generateOutfitPreview(any(), anyList(), anyString(), any(AiLanguage.class)))
        .thenAnswer(invocation -> {
          AiLanguage language = invocation.getArgument(3);
          return switch (language) {
            case ZH -> new OutfitPreviewResponse("ZH_TITLE", "ZH_DESC", "ZH_PROMPT");
            case KO -> new OutfitPreviewResponse("KO_TITLE", "KO_DESC", "KO_PROMPT");
            default -> new OutfitPreviewResponse("EN_TITLE", "EN_DESC", "EN_PROMPT");
          };
        });
  }

  @Test
  void taskShouldEventuallySucceedAndWriteHistory() throws Exception {
    String token = loginAndGetAccessToken();
    long memberId = createMember(token, "PreviewReadyMember", "https://img.test/member-ready.jpg");
    long top1 = createClothing(token, "PreviewReadyTop1", ClothingType.TOP, "https://img.test/top-1.jpg");
    long bottom1 = createClothing(token, "PreviewReadyBottom1", ClothingType.BOTTOM, "https://img.test/bottom-1.jpg");
    long top2 = createClothing(token, "PreviewReadyTop2", ClothingType.TOP, "https://img.test/top-2.jpg");
    long bottom2 = createClothing(token, "PreviewReadyBottom2", ClothingType.BOTTOM, "https://img.test/bottom-2.jpg");
    long top3 = createClothing(token, "PreviewReadyTop3", ClothingType.TOP, "https://img.test/top-3.jpg");
    long bottom3 = createClothing(token, "PreviewReadyBottom3", ClothingType.BOTTOM, "https://img.test/bottom-3.jpg");
    String taskId = createTask(token, memberId, List.of(top1, bottom1, top2, bottom2, top3, bottom3), "daily-live");

    String finalStatus = null;
    for (int i = 0; i < 12; i++) {
      Thread.sleep(250);
      String taskJson = mockMvc.perform(get("/api/match/tasks/{taskId}", taskId)
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andReturn()
          .getResponse()
          .getContentAsString();
      JsonNode taskRoot = objectMapper.readTree(taskJson);
      finalStatus = taskRoot.path("data").path("status").asText();
      if ("SUCCEEDED".equals(finalStatus) || "FAILED".equals(finalStatus)) {
        break;
      }
    }

    assertThat(finalStatus).isEqualTo("SUCCEEDED");

    String taskDoneJson = mockMvc.perform(get("/api/match/tasks/{taskId}", taskId)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    JsonNode doneRoot = objectMapper.readTree(taskDoneJson);
    assertThat(doneRoot.path("data").path("strategyName").asText()).isEqualTo("AI_ONLY");
    assertThat(doneRoot.path("data").path("outfits").isArray()).isTrue();
    assertThat(doneRoot.path("data").path("outfits").size()).isEqualTo(3);
    assertThat(doneRoot.path("data").path("outfits").get(0).path("preview").isNull()).isTrue();
    assertThat(doneRoot.path("data").path("result").isArray()).isTrue();
    assertThat(doneRoot.path("data").path("result").size()).isEqualTo(6);
    assertThat(doneRoot.path("data").path("preview").isNull()).isTrue();

    JsonNode previewRoot = generateOutfitPreview(token, taskId, 1);
    assertThat(previewRoot.path("data").path("outfits").get(0).path("preview").isObject()).isTrue();
    assertThat(previewRoot.path("data").path("outfits").get(0).path("preview").path("title").asText()).isNotBlank();
    assertThat(previewRoot.path("data").path("preview").isObject()).isTrue();

    String historyJson = mockMvc.perform(get("/api/members/{memberId}/history", memberId)
            .header("Authorization", "Bearer " + token)
            .param("limit", "10"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode historyRoot = objectMapper.readTree(historyJson);
    assertThat(historyRoot.path("data").path("records").isArray()).isTrue();
    assertThat(historyRoot.path("data").path("total").asLong()).isPositive();
  }

  @Test
  void taskShouldSucceedWhenLessThanThreeOutfitsAreAvailable() throws Exception {
    String token = loginAndGetAccessToken();
    long memberId = createMember(token, "NoSizeMatchMember", null);
    long topId = createClothing(token, "OnlyTopCandidate", ClothingType.TOP, null);
    long bottomId = createClothing(token, "OnlyBottomCandidate", ClothingType.BOTTOM, null);
    String taskId = createTask(token, memberId, List.of(topId, bottomId), "daily-live");

    String finalStatus = null;
    for (int i = 0; i < 20; i++) {
      Thread.sleep(250);
      String taskJson = mockMvc.perform(get("/api/match/tasks/{taskId}", taskId)
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andReturn()
          .getResponse()
          .getContentAsString();
      JsonNode taskRoot = objectMapper.readTree(taskJson);
      finalStatus = taskRoot.path("data").path("status").asText();
      if ("SUCCEEDED".equals(finalStatus) || "FAILED".equals(finalStatus)) {
        break;
      }
    }
    assertThat(finalStatus).isEqualTo("SUCCEEDED");

    String doneTask = mockMvc.perform(get("/api/match/tasks/{taskId}", taskId)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    JsonNode doneRoot = objectMapper.readTree(doneTask);
    assertThat(doneRoot.path("data").path("outfits").isArray()).isTrue();
    assertThat(doneRoot.path("data").path("outfits").size()).isEqualTo(1);
    assertThat(doneRoot.path("data").path("result").isArray()).isTrue();
    assertThat(doneRoot.path("data").path("result").size()).isEqualTo(2);
    assertThat(doneRoot.path("data").path("preview").isNull()).isTrue();
    assertThat(doneRoot.path("data").path("errorMessage").isNull()).isTrue();
  }

  @Test
  void taskShouldFailWhenAiSuggestionThrows() throws Exception {
    when(aiClientRouter.suggest(any(), anyList(), anyList(), anyString(), any(AiLanguage.class)))
        .thenThrow(new IllegalStateException("AI suggestion down"));
    String token = loginAndGetAccessToken();
    long memberId = createMember(token, "AiFailureMember", null);
    long topId = createClothing(token, "AiFailureTop", ClothingType.TOP, null);
    long bottomId = createClothing(token, "AiFailureBottom", ClothingType.BOTTOM, null);
    String taskId = createTask(token, memberId, List.of(topId, bottomId), "daily-live");

    JsonNode doneRoot = waitTaskDone(token, taskId);
    assertThat(doneRoot.path("data").path("status").asText()).isEqualTo("FAILED");
    assertThat(doneRoot.path("data").path("errorMessage").asText()).contains("AI suggestion down");
  }

  @Test
  void taskShouldFailWhenAiReturnsEmptyRecommendation() throws Exception {
    when(aiClientRouter.suggest(any(), anyList(), anyList(), anyString(), any(AiLanguage.class))).thenReturn(List.of());
    String token = loginAndGetAccessToken();
    long memberId = createMember(token, "AiEmptyMember", null);
    long topId = createClothing(token, "AiEmptyTop", ClothingType.TOP, null);
    long bottomId = createClothing(token, "AiEmptyBottom", ClothingType.BOTTOM, null);
    String taskId = createTask(token, memberId, List.of(topId, bottomId), "daily-live");

    JsonNode doneRoot = waitTaskDone(token, taskId);
    assertThat(doneRoot.path("data").path("status").asText()).isEqualTo("FAILED");
    assertThat(doneRoot.path("data").path("errorMessage").asText()).contains("AI outfit recommendation unavailable or invalid");
  }

  @Test
  void taskShouldSucceedWhenSingleOutfitPreviewFails() throws Exception {
    when(aiClientRouter.generateOutfitPreview(any(), anyList(), anyString(), any(AiLanguage.class))).thenReturn(null);
    String token = loginAndGetAccessToken();
    long memberId = createMember(token, "PreviewFailureMember", "https://img.test/member-preview-failure.jpg");
    long topId = createClothing(token, "PreviewFailureTop", ClothingType.TOP, "https://img.test/cloth-preview-failure-top.jpg");
    long bottomId = createClothing(token, "PreviewFailureBottom", ClothingType.BOTTOM, "https://img.test/cloth-preview-failure-bottom.jpg");
    String taskId = createTask(token, memberId, List.of(topId, bottomId), "daily-live");

    JsonNode doneRoot = waitTaskDone(token, taskId);
    assertThat(doneRoot.path("data").path("status").asText()).isEqualTo("SUCCEEDED");
    assertThat(doneRoot.path("data").path("outfits").isArray()).isTrue();
    assertThat(doneRoot.path("data").path("outfits").get(0).path("preview").isNull()).isTrue();
    assertThat(doneRoot.path("data").path("errorMessage").isNull()).isTrue();

    JsonNode afterPreview = generateOutfitPreview(token, taskId, 1);
    assertThat(afterPreview.path("data").path("status").asText()).isEqualTo("SUCCEEDED");
    assertThat(afterPreview.path("data").path("outfits").get(0).path("preview").isNull()).isTrue();
    assertThat(afterPreview.path("data").path("outfits").get(0).path("warning").asText())
        .contains("Preview skipped for outfit #1");
    assertThat(afterPreview.path("data").path("errorMessage").asText()).contains("Preview skipped for outfit #1");
  }

  @Test
  void taskShouldSucceedWithDegradedPreviewWhenClothingImageMissing() throws Exception {
    String token = loginAndGetAccessToken();
    long memberId = createMember(token, "DegradedByClothingImage", "https://img.test/member-has-photo.jpg");
    long topId = createClothing(token, "MissingTopImage", ClothingType.TOP, null);
    long bottomId = createClothing(token, "BottomWithImage", ClothingType.BOTTOM, "https://img.test/bottom-with-image.jpg");
    String taskId = createTask(token, memberId, List.of(topId, bottomId), "daily-live");

    JsonNode doneRoot = waitTaskDone(token, taskId);
    assertThat(doneRoot.path("data").path("status").asText()).isEqualTo("SUCCEEDED");
    assertThat(doneRoot.path("data").path("preview").isNull()).isTrue();
    assertThat(doneRoot.path("data").path("errorMessage").isNull()).isTrue();

    JsonNode afterPreview = generateOutfitPreview(token, taskId, 1);
    assertThat(afterPreview.path("data").path("status").asText()).isEqualTo("SUCCEEDED");
    assertThat(afterPreview.path("data").path("outfits").get(0).path("preview").isNull()).isTrue();
    assertThat(afterPreview.path("data").path("outfits").get(0).path("warning").asText())
        .contains("Preview skipped for outfit #1: Preview skipped: missing member photo or clothing image");
    assertThat(afterPreview.path("data").path("errorMessage").asText())
        .contains("Preview skipped for outfit #1: Preview skipped: missing member photo or clothing image");
  }

  @Test
  void taskReasonAndPreviewShouldFollowRequestLanguage() throws Exception {
    String token = loginAndGetAccessToken();
    long memberId = createMember(token, "LanguageMember", "https://img.test/member-language.jpg");
    long topId = createClothing(token, "LanguageTop", ClothingType.TOP, "https://img.test/language-top.jpg");
    long bottomId = createClothing(token, "LanguageBottom", ClothingType.BOTTOM, "https://img.test/language-bottom.jpg");

    String taskId = createTask(token, memberId, List.of(topId, bottomId), "daily-live", "zh-CN");
    JsonNode doneRoot = waitTaskDone(token, taskId);
    assertThat(doneRoot.path("data").path("status").asText()).isEqualTo("SUCCEEDED");
    assertThat(doneRoot.path("data").path("outfits").get(0).path("reason").asText()).isEqualTo("ZH_REASON");

    JsonNode previewEn = generateOutfitPreview(token, taskId, 1, "en-US");
    assertThat(previewEn.path("data").path("outfits").get(0).path("preview").path("title").asText()).isEqualTo("EN_TITLE");

    JsonNode previewKo = generateOutfitPreview(token, taskId, 1, "ko-KR");
    assertThat(previewKo.path("data").path("outfits").get(0).path("preview").path("title").asText()).isEqualTo("KO_TITLE");
  }

  private long createMember(String token, String name, String photoUrl) throws Exception {
    String photoJson = photoUrl == null ? "null" : "\"" + photoUrl + "\"";
    String body = mockMvc.perform(post("/api/members")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "%s",
                  "bodyData": "{\\"version\\":2,\\"measurements\\":{\\"heightCm\\":170,\\"weightKg\\":52,\\"shoulderWidthCm\\":38,\\"bustCm\\":86,\\"waistCm\\":64,\\"hipCm\\":90,\\"bodyShape\\":\\"H\\",\\"legRatio\\":\\"regular\\",\\"topSize\\":\\"M\\",\\"bottomSize\\":\\"M\\"}}",
                  "photoUrl": %s,
                  "styleTags": "casual"
                }
                """.formatted(name, photoJson)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return objectMapper.readTree(body).path("data").path("id").asLong();
  }

  private long createClothing(String token, String name, ClothingType clothingType, String imageUrl) throws Exception {
    String imageJson = imageUrl == null ? "null" : "\"" + imageUrl + "\"";
    String body = mockMvc.perform(post("/api/clothing")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "%s",
                  "imageUrl": %s,
                  "styleTags": "casual",
                  "clothingType": "%s",
                  "status": "ON_SHELF"
                }
                """.formatted(name, imageJson, clothingType.name())))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return objectMapper.readTree(body).path("data").path("id").asLong();
  }

  private String createTask(String token, long memberId, List<Long> clothingIds, String scene) throws Exception {
    return createTask(token, memberId, clothingIds, scene, null);
  }

  private String createTask(String token, long memberId, List<Long> clothingIds, String scene, String language)
      throws Exception {
    var request = post("/api/match/tasks")
        .header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "memberId": %d,
              "clothingIds": %s,
              "scene": "%s"
            }
            """.formatted(memberId, objectMapper.writeValueAsString(clothingIds), scene));
    if (language != null) {
      request.header("Accept-Language", language);
    }
    String body = mockMvc.perform(request)
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return objectMapper.readTree(body).path("data").path("taskId").asText();
  }

  private JsonNode waitTaskDone(String token, String taskId) throws Exception {
    String finalStatus = null;
    JsonNode taskRoot = null;
    for (int i = 0; i < 20; i++) {
      Thread.sleep(250);
      String taskJson = mockMvc.perform(get("/api/match/tasks/{taskId}", taskId)
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andReturn()
          .getResponse()
          .getContentAsString();
      taskRoot = objectMapper.readTree(taskJson);
      finalStatus = taskRoot.path("data").path("status").asText();
      if ("SUCCEEDED".equals(finalStatus) || "FAILED".equals(finalStatus)) {
        return taskRoot;
      }
    }
    return taskRoot;
  }

  private JsonNode generateOutfitPreview(String token, String taskId, int outfitNo) throws Exception {
    return generateOutfitPreview(token, taskId, outfitNo, null);
  }

  private JsonNode generateOutfitPreview(String token, String taskId, int outfitNo, String language) throws Exception {
    var request = post("/api/match/tasks/{taskId}/outfits/{outfitNo}/preview", taskId, outfitNo)
        .header("Authorization", "Bearer " + token);
    if (language != null) {
      request.header("Accept-Language", language);
    }
    String response = mockMvc.perform(request)
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return objectMapper.readTree(response);
  }

  private String loginAndGetAccessToken() throws Exception {
    String loginJson = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "username": "stylist",
                  "password": "stylist123"
                }
                """))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode loginRoot = objectMapper.readTree(loginJson);
    return loginRoot.path("data").path("accessToken").asText();
  }
}
