package com.company.fashion.modules.clothing.controller;

import com.company.fashion.common.api.PageResponse;
import com.company.fashion.common.api.Result;
import com.company.fashion.modules.clothing.dto.ClothingItemResponse;
import com.company.fashion.modules.clothing.dto.ClothingPageResponse;
import com.company.fashion.modules.clothing.dto.CreateClothingRequest;
import com.company.fashion.modules.clothing.dto.UpdateClothingRequest;
import com.company.fashion.modules.clothing.dto.UpdateClothingStatusRequest;
import com.company.fashion.modules.clothing.entity.ClothingStatus;
import com.company.fashion.modules.clothing.service.ClothingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clothing")
@Tag(name = "Clothing", description = "Clothing inventory management APIs")
@SecurityRequirement(name = "bearerAuth")
public class ClothingController {

  private final ClothingService clothingService;

  public ClothingController(ClothingService clothingService) {
    this.clothingService = clothingService;
  }

  @PostMapping
  @Operation(summary = "Create clothing item (clothingType is required and only TOP/BOTTOM are accepted)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Create success"),
      @ApiResponse(responseCode = "400", description = "Invalid clothingType")
  })
  public Result<ClothingItemResponse> create(@Valid @RequestBody CreateClothingRequest request) {
    return Result.ok(clothingService.create(request));
  }

  @GetMapping
  @Operation(summary = "List ON_SHELF/OFF_SHELF clothing by status")
  public Result<ClothingPageResponse> listOnShelf(
      @Parameter(description = "Status filter, default ON_SHELF") @RequestParam(required = false) ClothingStatus status,
      @Parameter(description = "Page number, starts from 0") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size
  ) {
    return Result.ok(clothingService.list(status, page, size));
  }

  @GetMapping("/all")
  @Operation(summary = "List all clothing (excluding soft deleted)")
  public Result<PageResponse<ClothingItemResponse>> listAll(
      @Parameter(description = "Page number, starts from 0") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size
  ) {
    return Result.ok(clothingService.listAll(page, size));
  }

  @GetMapping("/{clothingId}")
  @Operation(summary = "Get clothing detail")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Query success"),
      @ApiResponse(responseCode = "404", description = "Clothing not found")
  })
  public Result<ClothingItemResponse> get(
      @Parameter(description = "Clothing ID", required = true) @PathVariable Long clothingId
  ) {
    return Result.ok(clothingService.get(clothingId));
  }

  @PutMapping("/{clothingId}")
  @Operation(summary = "Update clothing basic info (clothingType is required and only TOP/BOTTOM are accepted)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Update success"),
      @ApiResponse(responseCode = "400", description = "Invalid clothingType")
  })
  public Result<ClothingItemResponse> update(
      @Parameter(description = "Clothing ID", required = true) @PathVariable Long clothingId,
      @Valid @RequestBody UpdateClothingRequest request
  ) {
    return Result.ok(clothingService.update(clothingId, request));
  }

  @PatchMapping("/{clothingId}/status")
  @Operation(summary = "Update clothing status ON_SHELF/OFF_SHELF")
  public Result<ClothingItemResponse> updateStatus(
      @Parameter(description = "Clothing ID", required = true) @PathVariable Long clothingId,
      @Valid @RequestBody UpdateClothingStatusRequest request
  ) {
    return Result.ok(clothingService.updateStatus(clothingId, request.status()));
  }

  @DeleteMapping("/{clothingId}")
  @Operation(summary = "Delete clothing (soft delete)")
  public Result<Void> delete(
      @Parameter(description = "Clothing ID", required = true) @PathVariable Long clothingId
  ) {
    clothingService.delete(clothingId);
    return Result.ok();
  }
}
