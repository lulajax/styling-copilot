package com.company.fashion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.fashion.modules.clothing.entity.Clothing;
import com.company.fashion.modules.clothing.entity.ClothingType;
import com.company.fashion.modules.match.ai.AiLanguage;
import com.company.fashion.modules.match.ai.AiClientRouter;
import com.company.fashion.modules.match.dto.OutfitPreviewResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
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
class MatchTaskPolicyIntegrationTest {

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
                "AI policy test recommendation"
            ));
          }
          return result;
        });
    when(aiClientRouter.generateOutfitPreview(any(), anyList(), anyString(), any(AiLanguage.class)))
        .thenReturn(new OutfitPreviewResponse(
            "AI Preview",
            "AI generated outfit description",
            "AI generated image prompt"
        ));
  }

  @Test
  void shouldApplySevenDayDedup() throws Exception {
    String token = loginAndGetAccessToken();
    long memberId = createMember(token, "DedupMember");
    long topId = createClothing(token, "DedupTop", ClothingType.TOP);
    long bottomId = createClothing(token, "DedupBottom", ClothingType.BOTTOM);

    String firstTaskId = createTask(token, memberId, List.of(topId, bottomId));
    String firstTaskStatus = waitTaskDone(token, firstTaskId);
    assertThat(firstTaskStatus).isEqualTo("SUCCEEDED");

    for (long recordId : fetchHistoryRecordIds(token, memberId, 10)) {
      mockMvc.perform(patch("/api/members/{memberId}/history/{recordId}/status", memberId, recordId)
              .header("Authorization", "Bearer " + token)
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  {
                    "status": "BROADCASTED"
                  }
                  """))
          .andExpect(status().isOk());
    }

    String secondResponse = mockMvc.perform(post("/api/match/tasks")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "memberId": %d,
                  "clothingIds": [%d, %d],
                  "scene": "daily-live"
                }
                """.formatted(memberId, topId, bottomId)))
        .andExpect(status().isBadRequest())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode root = objectMapper.readTree(secondResponse);
    assertThat(root.path("message").asText()).contains("worn history");
  }

  @Test
  void shouldNotDedupWhenHistoryStillDraft() throws Exception {
    String token = loginAndGetAccessToken();
    long memberId = createMember(token, "DraftHistoryMember");
    long topId = createClothing(token, "DraftHistoryTop", ClothingType.TOP);
    long bottomId = createClothing(token, "DraftHistoryBottom", ClothingType.BOTTOM);

    String firstTaskId = createTask(token, memberId, List.of(topId, bottomId));
    assertThat(waitTaskDone(token, firstTaskId)).isEqualTo("SUCCEEDED");

    String secondTaskId = createTask(token, memberId, List.of(topId, bottomId));
    assertThat(waitTaskDone(token, secondTaskId)).isEqualTo("SUCCEEDED");
  }

  @Test
  void shouldDedupWhenManualWornHistoryCreated() throws Exception {
    String token = loginAndGetAccessToken();
    long memberId = createMember(token, "ManualWornMember");
    long topId = createClothing(token, "ManualWornTop", ClothingType.TOP);
    long bottomId = createClothing(token, "ManualWornBottom", ClothingType.BOTTOM);

    mockMvc.perform(post("/api/members/{memberId}/history/manual", memberId)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "clothingId": %d
                }
                """.formatted(topId)))
        .andExpect(status().isOk());
    mockMvc.perform(post("/api/members/{memberId}/history/manual", memberId)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "clothingId": %d
                }
                """.formatted(bottomId)))
        .andExpect(status().isOk());

    mockMvc.perform(post("/api/match/tasks")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "memberId": %d,
                  "clothingIds": [%d, %d],
                  "scene": "daily-live"
                }
                """.formatted(memberId, topId, bottomId)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldAllowTaskAfterMarkedAsUnworn() throws Exception {
    String token = loginAndGetAccessToken();
    long memberId = createMember(token, "UnwornMember");
    long topId = createClothing(token, "UnwornTop", ClothingType.TOP);
    long bottomId = createClothing(token, "UnwornBottom", ClothingType.BOTTOM);

    mockMvc.perform(post("/api/members/{memberId}/history/manual", memberId)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "clothingId": %d
                }
                """.formatted(topId)))
        .andExpect(status().isOk());
    mockMvc.perform(post("/api/members/{memberId}/history/manual", memberId)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "clothingId": %d
                }
                """.formatted(bottomId)))
        .andExpect(status().isOk());

    mockMvc.perform(post("/api/match/tasks")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "memberId": %d,
                  "clothingIds": [%d, %d],
                  "scene": "daily-live"
                }
                """.formatted(memberId, topId, bottomId)))
        .andExpect(status().isBadRequest());

    for (long recordId : fetchHistoryRecordIds(token, memberId, 10)) {
      mockMvc.perform(patch("/api/members/{memberId}/history/{recordId}/status", memberId, recordId)
              .header("Authorization", "Bearer " + token)
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  {
                    "status": "DRAFT"
                  }
                  """))
          .andExpect(status().isOk());
    }

    String taskId = createTask(token, memberId, List.of(topId, bottomId));
    assertThat(waitTaskDone(token, taskId)).isEqualTo("SUCCEEDED");
  }

  @Test
  void shouldRejectWhenRateLimitExceeded() throws Exception {
    String token = loginAndGetAccessToken();
    long memberId = createMember(token, "RateMember");
    long top1 = createClothing(token, "RateTop1", ClothingType.TOP);
    long bottom1 = createClothing(token, "RateBottom1", ClothingType.BOTTOM);
    long top2 = createClothing(token, "RateTop2", ClothingType.TOP);

    for (int i = 0; i < 3; i++) {
      mockMvc.perform(post("/api/match/tasks")
              .header("Authorization", "Bearer " + token)
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  {
                    "memberId": %d,
                    "clothingIds": [%d, %d, %d],
                    "scene": "daily-live"
                  }
                  """.formatted(memberId, top1, bottom1, top2)))
          .andExpect(status().isOk());
    }

    String blocked = mockMvc.perform(post("/api/match/tasks")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "memberId": %d,
                  "clothingIds": [%d, %d, %d],
                  "scene": "daily-live"
                }
                """.formatted(memberId, top1, bottom1, top2)))
        .andExpect(status().isTooManyRequests())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode root = objectMapper.readTree(blocked);
    assertThat(root.path("message").asText()).contains("rate limit");
  }

  private String waitTaskDone(String token, String taskId) throws Exception {
    String taskStatus = "";
    for (int i = 0; i < 20; i++) {
      Thread.sleep(250);
      String body = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
              .get("/api/match/tasks/{taskId}", taskId)
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andReturn()
          .getResponse()
          .getContentAsString();
      JsonNode root = objectMapper.readTree(body);
      taskStatus = root.path("data").path("status").asText();
      if ("SUCCEEDED".equals(taskStatus) || "FAILED".equals(taskStatus)) {
        return taskStatus;
      }
    }
    return taskStatus;
  }

  private List<Long> fetchHistoryRecordIds(String token, long memberId, int limit) throws Exception {
    String body = mockMvc.perform(get("/api/members/{memberId}/history", memberId)
            .header("Authorization", "Bearer " + token)
            .param("limit", String.valueOf(limit)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    JsonNode root = objectMapper.readTree(body);
    List<Long> ids = new ArrayList<>();
    for (JsonNode item : root.path("data").path("records")) {
      ids.add(item.path("id").asLong());
    }
    return ids;
  }

  private long createMember(String token, String name) throws Exception {
    String body = mockMvc.perform(post("/api/members")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "%s",
                  "bodyData": "{\\"version\\":2,\\"measurements\\":{\\"heightCm\\":170,\\"weightKg\\":52,\\"shoulderWidthCm\\":38,\\"bustCm\\":86,\\"waistCm\\":64,\\"hipCm\\":90,\\"bodyShape\\":\\"H\\",\\"legRatio\\":\\"regular\\",\\"topSize\\":\\"M\\",\\"bottomSize\\":\\"M\\"}}",
                  "styleTags": "casual"
                }
                """.formatted(name)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return objectMapper.readTree(body).path("data").path("id").asLong();
  }

  private long createClothing(String token, String name, ClothingType type) throws Exception {
    String body = mockMvc.perform(post("/api/clothing")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "%s",
                  "styleTags": "casual",
                  "clothingType": "%s",
                  "status": "ON_SHELF"
                }
                """.formatted(name, type.name())))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return objectMapper.readTree(body).path("data").path("id").asLong();
  }

  private String createTask(String token, long memberId, List<Long> clothingIds) throws Exception {
    String body = mockMvc.perform(post("/api/match/tasks")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "memberId": %d,
                  "clothingIds": %s,
                  "scene": "daily-live"
                }
                """.formatted(memberId, objectMapper.writeValueAsString(clothingIds))))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return objectMapper.readTree(body).path("data").path("taskId").asText();
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
