package com.company.fashion.modules.file.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.company.fashion.common.exception.BusinessException;
import com.company.fashion.config.OssProperties;
import java.io.InputStream;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OssStorageService {

  private final OssProperties ossProperties;

  public OssStorageService(OssProperties ossProperties) {
    this.ossProperties = ossProperties;
  }

  public String upload(String objectKey, MultipartFile file) {
    ensureConfigured();

    OSS ossClient = null;
    try (InputStream inputStream = file.getInputStream()) {
      ossClient = new OSSClientBuilder().build(
          ossProperties.getEndpoint(),
          ossProperties.getAccessKeyId(),
          ossProperties.getAccessKeySecret()
      );

      ObjectMetadata metadata = new ObjectMetadata();
      if (file.getContentType() != null && !file.getContentType().isBlank()) {
        metadata.setContentType(file.getContentType());
      }
      metadata.setContentLength(file.getSize());
      ossClient.putObject(ossProperties.getBucket(), objectKey, inputStream, metadata);
      return buildPublicUrl(objectKey);
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException(500, "Upload to OSS failed: " + ex.getMessage());
    } finally {
      if (ossClient != null) {
        ossClient.shutdown();
      }
    }
  }

  private void ensureConfigured() {
    if (isBlank(ossProperties.getEndpoint())
        || isBlank(ossProperties.getBucket())
        || isBlank(ossProperties.getAccessKeyId())
        || isBlank(ossProperties.getAccessKeySecret())) {
      throw new BusinessException(500, "OSS is not configured");
    }
  }

  private String buildPublicUrl(String objectKey) {
    if (!isBlank(ossProperties.getPublicBaseUrl())) {
      return trimSlash(ossProperties.getPublicBaseUrl()) + "/" + objectKey;
    }
    String endpointHost = ossProperties.getEndpoint()
        .replaceFirst("^https?://", "")
        .replaceAll("/+$", "");
    return "https://" + ossProperties.getBucket() + "." + endpointHost + "/" + objectKey;
  }

  private String trimSlash(String value) {
    return value.replaceAll("/+$", "");
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
