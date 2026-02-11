package com.company.fashion.modules.file.service;

import com.company.fashion.common.exception.BusinessException;
import com.company.fashion.config.OssProperties;
import com.company.fashion.modules.file.dto.UploadFileResponse;
import com.company.fashion.modules.file.model.UploadBizType;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileUploadService {

  private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
  private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

  private final OssStorageService ossStorageService;
  private final OssProperties ossProperties;

  public FileUploadService(OssStorageService ossStorageService, OssProperties ossProperties) {
    this.ossStorageService = ossStorageService;
    this.ossProperties = ossProperties;
  }

  public UploadFileResponse upload(MultipartFile file, UploadBizType bizType) {
    validate(file);
    String originalName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
    String extension = getExtension(originalName);
    String objectKey = buildObjectKey(bizType, originalName, extension);
    String url = ossStorageService.upload(objectKey, file);

    return new UploadFileResponse(
        url,
        objectKey,
        bizType,
        originalName,
        file.getSize(),
        file.getContentType()
    );
  }

  private void validate(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(400, "file is required");
    }

    if (file.getSize() > MAX_FILE_SIZE_BYTES) {
      throw new BusinessException(400, "file size exceeds 5MB");
    }

    String originalName = file.getOriginalFilename();
    String extension = getExtension(originalName == null ? "" : originalName);
    if (!ALLOWED_EXTENSIONS.contains(extension)) {
      throw new BusinessException(400, "Unsupported file type, allowed: jpg/jpeg/png/webp");
    }
  }

  private String buildObjectKey(UploadBizType bizType, String originalName, String extension) {
    LocalDate today = LocalDate.now();
    String safeName = safeName(stripExtension(originalName));
    String prefix = safeName.isBlank() ? "file" : safeName;
    String baseDir = ossProperties.getDirPrefix() == null || ossProperties.getDirPrefix().isBlank()
        ? "fashion"
        : safeName(ossProperties.getDirPrefix());

    return "%s/%s/%d/%02d/%02d/%s-%s.%s".formatted(
        baseDir,
        bizType.getValue(),
        today.getYear(),
        today.getMonthValue(),
        today.getDayOfMonth(),
        UUID.randomUUID(),
        prefix,
        extension
    );
  }

  private String getExtension(String filename) {
    int dotIndex = filename.lastIndexOf('.');
    if (dotIndex < 0 || dotIndex == filename.length() - 1) {
      return "";
    }
    return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
  }

  private String stripExtension(String filename) {
    int dotIndex = filename.lastIndexOf('.');
    if (dotIndex < 0) {
      return filename;
    }
    return filename.substring(0, dotIndex);
  }

  private String safeName(String value) {
    return value == null ? "" : value.replaceAll("[^a-zA-Z0-9._-]", "_");
  }
}
