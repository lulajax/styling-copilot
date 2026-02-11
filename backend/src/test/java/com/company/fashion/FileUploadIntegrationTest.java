package com.company.fashion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.fashion.modules.file.service.OssStorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class FileUploadIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private OssStorageService ossStorageService;

  @Test
  void shouldUploadFileWhenRequestIsValid() throws Exception {
    when(ossStorageService.upload(anyString(), any())).thenAnswer(invocation ->
        "https://cdn.example.com/" + invocation.getArgument(0, String.class)
    );

    String token = loginAndGetAccessToken();
    MockMultipartFile file = new MockMultipartFile("file", "preview.jpg", "image/jpeg", "image-bytes".getBytes());

    String response = mockMvc.perform(multipart("/api/files/upload")
            .file(file)
            .param("bizType", "member")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode root = objectMapper.readTree(response);
    assertThat(root.path("data").path("bizType").asText()).isEqualTo("member");
    assertThat(root.path("data").path("objectKey").asText()).contains("/member/");
    assertThat(root.path("data").path("url").asText()).startsWith("https://cdn.example.com/");
  }

  @Test
  void shouldRejectUnsupportedFileType() throws Exception {
    String token = loginAndGetAccessToken();
    MockMultipartFile file = new MockMultipartFile("file", "preview.txt", "text/plain", "text".getBytes());

    String response = mockMvc.perform(multipart("/api/files/upload")
            .file(file)
            .param("bizType", "member")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode root = objectMapper.readTree(response);
    assertThat(root.path("message").asText()).contains("Unsupported file type");
  }

  @Test
  void shouldRejectWhenBizTypeMissing() throws Exception {
    String token = loginAndGetAccessToken();
    MockMultipartFile file = new MockMultipartFile("file", "preview.jpg", "image/jpeg", "image".getBytes());

    String response = mockMvc.perform(multipart("/api/files/upload")
            .file(file)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode root = objectMapper.readTree(response);
    assertThat(root.path("message").asText()).contains("Required request parameter");
  }

  @Test
  void shouldRejectWhenFileTooLarge() throws Exception {
    String token = loginAndGetAccessToken();
    byte[] bytes = new byte[5 * 1024 * 1024 + 1];
    MockMultipartFile file = new MockMultipartFile("file", "large.jpg", "image/jpeg", bytes);

    String response = mockMvc.perform(multipart("/api/files/upload")
            .file(file)
            .param("bizType", "clothing")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode root = objectMapper.readTree(response);
    assertThat(root.path("message").asText()).contains("5MB");
  }

  private String loginAndGetAccessToken() throws Exception {
    String loginJson = mockMvc.perform(post("/api/auth/login")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
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

