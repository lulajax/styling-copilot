package com.company.fashion.modules.match.controller;

import com.company.fashion.common.api.Result;
import com.company.fashion.modules.match.dto.CreateManualHistoryRequest;
import com.company.fashion.modules.match.dto.MatchHistoryResponse;
import com.company.fashion.modules.match.dto.MatchHistoryItemResponse;
import com.company.fashion.modules.match.dto.UpdateMatchRecordStatusRequest;
import com.company.fashion.modules.match.service.MatchTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@Validated
@Tag(name = "MatchHistory", description = "Member match history APIs")
@SecurityRequirement(name = "bearerAuth")
public class MatchHistoryController {

  private final MatchTaskService matchTaskService;

  public MatchHistoryController(MatchTaskService matchTaskService) {
    this.matchTaskService = matchTaskService;
  }

  @GetMapping("/{memberId}/history")
  @Operation(summary = "Get member match history")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Query success"),
      @ApiResponse(responseCode = "404", description = "Member not found")
  })
  public Result<MatchHistoryResponse> history(
      @Parameter(description = "Member ID", required = true) @PathVariable Long memberId,
      @Parameter(description = "History records limit, range [1,100]")
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
  ) {
    return Result.ok(matchTaskService.getHistory(memberId, limit));
  }

  @PostMapping("/{memberId}/history/manual")
  @Operation(summary = "Create a manual worn history record")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Create success"),
      @ApiResponse(responseCode = "404", description = "Member or clothing not found")
  })
  public Result<MatchHistoryItemResponse> createManualHistory(
      @Parameter(description = "Member ID", required = true) @PathVariable Long memberId,
      @Valid @RequestBody CreateManualHistoryRequest request
  ) {
    return Result.ok(matchTaskService.createManualHistory(memberId, request));
  }

  @PatchMapping("/{memberId}/history/{recordId}/status")
  @Operation(summary = "Update history status, e.g. mark record as BROADCASTED")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Update success"),
      @ApiResponse(responseCode = "404", description = "Member or history record not found")
  })
  public Result<MatchHistoryItemResponse> updateHistoryStatus(
      @Parameter(description = "Member ID", required = true) @PathVariable Long memberId,
      @Parameter(description = "History record ID", required = true) @PathVariable Long recordId,
      @Valid @RequestBody UpdateMatchRecordStatusRequest request
  ) {
    return Result.ok(matchTaskService.updateHistoryStatus(memberId, recordId, request));
  }
}
