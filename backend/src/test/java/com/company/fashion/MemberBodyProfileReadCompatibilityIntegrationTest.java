package com.company.fashion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.fashion.modules.member.entity.Member;
import com.company.fashion.modules.member.repository.MemberRepository;
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
class MemberBodyProfileReadCompatibilityIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private MemberRepository memberRepository;

  @Test
  void shouldReturnNormalizedV2BodyProfileForLegacyBodyData() throws Exception {
    Member legacy = new Member();
    legacy.setName("LegacyBodyMember");
    legacy.setBodyData("{\"height\":170,\"shape\":\"X\"}");
    legacy.setStyleTags("casual");
    legacy = memberRepository.save(legacy);

    String token = loginAndGetAccessToken();
    String response = mockMvc.perform(get("/api/members/{memberId}", legacy.getId())
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode root = objectMapper.readTree(response);
    String bodyData = root.path("data").path("bodyData").asText();
    JsonNode bodyNode = objectMapper.readTree(bodyData);

    assertThat(bodyNode.path("version").asInt()).isEqualTo(2);
    JsonNode measurements = bodyNode.path("measurements");
    assertThat(measurements.path("heightCm").asDouble()).isEqualTo(170.0);
    assertThat(measurements.path("weightKg").asDouble()).isPositive();
    assertThat(measurements.path("topSize").asText()).isEqualTo("M");
    assertThat(measurements.path("bottomSize").asText()).isEqualTo("M");
    assertThat(measurements.has("inseamCm")).isFalse();
    assertThat(measurements.has("torsoLengthCm")).isFalse();
    assertThat(measurements.has("neckCm")).isFalse();
    assertThat(measurements.has("thighCm")).isFalse();
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
