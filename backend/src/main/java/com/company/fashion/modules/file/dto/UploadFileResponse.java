package com.company.fashion.modules.file.dto;

import com.company.fashion.modules.file.model.UploadBizType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Upload result response")
public record UploadFileResponse(
    @Schema(description = "Public accessible file URL")
    String url,
    @Schema(description = "Stored object key in OSS")
    String objectKey,
    @Schema(description = "Business type")
    UploadBizType bizType,
    @Schema(description = "Original uploaded filename")
    String originalName,
    @Schema(description = "File size in bytes")
    long size,
    @Schema(description = "Content type")
    String contentType
) {
}
