package com.company.fashion.modules.file.controller;

import com.company.fashion.common.api.Result;
import com.company.fashion.modules.file.dto.UploadFileResponse;
import com.company.fashion.modules.file.model.UploadBizType;
import com.company.fashion.modules.file.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@Tag(name = "File", description = "File upload APIs")
@SecurityRequirement(name = "bearerAuth")
public class FileController {

  private final FileUploadService fileUploadService;

  public FileController(FileUploadService fileUploadService) {
    this.fileUploadService = fileUploadService;
  }

  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "Upload file to OSS")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Upload success"),
      @ApiResponse(responseCode = "400", description = "Invalid file input"),
      @ApiResponse(responseCode = "500", description = "OSS unavailable",
          content = @Content(schema = @Schema(type = "string")))
  })
  public Result<UploadFileResponse> upload(
      @Parameter(description = "Business type: member or clothing", required = true)
      @RequestParam String bizType,
      @Parameter(description = "File binary", required = true)
      @RequestParam("file") MultipartFile file
  ) {
    UploadBizType type = UploadBizType.from(bizType);
    return Result.ok(fileUploadService.upload(file, type));
  }
}
