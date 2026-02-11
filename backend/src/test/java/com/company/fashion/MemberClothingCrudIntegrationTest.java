package com.company.fashion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class MemberClothingCrudIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void shouldPersistMemberPhotoAndClothingImage() throws Exception {
    String token = loginAndGetAccessToken();

    String createMember = mockMvc.perform(post("/api/members")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "MemberWithPhoto",
                  "bodyData": "{\\"version\\":2,\\"measurements\\":{\\"heightCm\\":168,\\"weightKg\\":49,\\"shoulderWidthCm\\":38,\\"bustCm\\":84,\\"waistCm\\":62,\\"hipCm\\":89,\\"bodyShape\\":\\"X\\",\\"legRatio\\":\\"long\\",\\"topSize\\":\\"S\\",\\"bottomSize\\":\\"S\\"}}",
                  "photoUrl": "https://cdn.example.com/member/p1.jpg",
                  "styleTags": "kpop_sweet_cool,street_dance"
                }
                """))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    JsonNode memberNode = objectMapper.readTree(createMember).path("data");
    long memberId = memberNode.path("id").asLong();
    assertThat(memberNode.path("photoUrl").asText()).isEqualTo("https://cdn.example.com/member/p1.jpg");
    JsonNode createdBody = objectMapper.readTree(memberNode.path("bodyData").asText());
    assertThat(createdBody.path("version").asInt()).isEqualTo(2);
    assertThat(createdBody.path("derived").path("bmi").asDouble()).isPositive();

    String updateMember = mockMvc.perform(put("/api/members/{memberId}", memberId)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "photoUrl": "https://cdn.example.com/member/p2.jpg"
                }
                """))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    JsonNode updatedMember = objectMapper.readTree(updateMember).path("data");
    assertThat(updatedMember.path("photoUrl").asText()).isEqualTo("https://cdn.example.com/member/p2.jpg");
    JsonNode updatedBody = objectMapper.readTree(updatedMember.path("bodyData").asText());
    assertThat(updatedBody.path("version").asInt()).isEqualTo(2);

    String createClothing = mockMvc.perform(post("/api/clothing")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "ClothingWithImage",
                  "imageUrl": "https://cdn.example.com/clothing/c1.jpg",
                  "styleTags": "kpop_sweet_cool",
                  "clothingType": "TOP",
                  "status": "ON_SHELF"
                }
                """))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    JsonNode clothingNode = objectMapper.readTree(createClothing).path("data");
    long clothingId = clothingNode.path("id").asLong();
    assertThat(clothingNode.path("imageUrl").asText()).isEqualTo("https://cdn.example.com/clothing/c1.jpg");
    assertThat(clothingNode.path("clothingType").asText()).isEqualTo("TOP");

    String updateClothing = mockMvc.perform(put("/api/clothing/{clothingId}", clothingId)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "imageUrl": "https://cdn.example.com/clothing/c2.jpg",
                  "clothingType": "BOTTOM"
                }
                """))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    JsonNode updatedClothing = objectMapper.readTree(updateClothing).path("data");
    assertThat(updatedClothing.path("imageUrl").asText()).isEqualTo("https://cdn.example.com/clothing/c2.jpg");
    assertThat(updatedClothing.path("clothingType").asText()).isEqualTo("BOTTOM");
  }

  @Test
  void shouldRejectMemberWhenBodySizesMissing() throws Exception {
    String token = loginAndGetAccessToken();

    mockMvc.perform(post("/api/members")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "MemberWithoutSize",
                  "bodyData": "{\\"version\\":2,\\"measurements\\":{\\"heightCm\\":168,\\"weightKg\\":49,\\"shoulderWidthCm\\":38,\\"bustCm\\":84,\\"waistCm\\":62,\\"hipCm\\":89,\\"bodyShape\\":\\"X\\",\\"legRatio\\":\\"long\\"}}",
                  "styleTags": "kpop_sweet_cool"
                }
                """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldRejectWhenClothingTypeMissingOrInvalid() throws Exception {
    String token = loginAndGetAccessToken();

    mockMvc.perform(post("/api/clothing")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "MissingType",
                  "styleTags": "casual",
                  "status": "ON_SHELF"
                }
                """))
        .andExpect(status().isBadRequest());

    String created = mockMvc.perform(post("/api/clothing")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "ValidType",
                  "styleTags": "casual",
                  "clothingType": "TOP",
                  "status": "ON_SHELF"
                }
                """))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    long clothingId = objectMapper.readTree(created).path("data").path("id").asLong();

    mockMvc.perform(put("/api/clothing/{clothingId}", clothingId)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "InvalidUpdateWithoutType"
                }
                """))
        .andExpect(status().isBadRequest());

    mockMvc.perform(post("/api/clothing")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "InvalidTypeOnCreate",
                  "styleTags": "casual",
                  "clothingType": "SET",
                  "status": "ON_SHELF"
                }
                """))
        .andExpect(status().isBadRequest());
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
