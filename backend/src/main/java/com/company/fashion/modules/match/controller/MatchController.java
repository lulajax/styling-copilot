package com.company.fashion.modules.match.controller;

import com.company.fashion.common.api.PageResponse;
import com.company.fashion.common.api.Result;
import com.company.fashion.modules.match.dto.CreateMatchTaskRequest;
import com.company.fashion.modules.match.dto.CreateMatchTaskResponse;
import com.company.fashion.modules.match.dto.MatchTaskResultResponse;
import com.company.fashion.modules.match.dto.MatchTaskSummaryResponse;
import com.company.fashion.modules.match.service.MatchTaskService;
import com.company.fashion.modules.match.sse.SseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/match/tasks")
@Tag(name = "MatchTask", description = "Match task orchestration and query APIs")
@SecurityRequirement(name = "bearerAuth")
public class MatchController {

  private final MatchTaskService matchTaskService;
  private final SseService sseService;

  public MatchController(MatchTaskService matchTaskService, SseService sseService) {
    this.matchTaskService = matchTaskService;
    this.sseService = sseService;
  }

  @PostMapping
  @Operation(summary = "Create a match task", description = "Reason language follows Accept-Language (zh/en/ko).")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Task created"),
      @ApiResponse(responseCode = "400", description = "Invalid request or dedup filtered all candidates"),
      @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
  })
  public Result<CreateMatchTaskResponse> createTask(
      @Valid @RequestBody CreateMatchTaskRequest request,
      @Parameter(description = "Response language (zh/en/ko), defaults to en")
      @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
      Principal principal
  ) {
    if (principal == null) {
      throw new com.company.fashion.common.exception.BusinessException(401, "Unauthorized");
    }
    return Result.ok(matchTaskService.createTask(request, principal.getName(), acceptLanguage));
  }

  @GetMapping("/{taskId}")
  @Operation(summary = "Get task detail and result")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Query success"),
      @ApiResponse(responseCode = "404", description = "Task not found")
  })
  public Result<MatchTaskResultResponse> getTask(
      @Parameter(description = "Task ID", required = true) @PathVariable String taskId
  ) {
    return Result.ok(matchTaskService.getTask(taskId));
  }

  @PostMapping("/{taskId}/outfits/{outfitNo}/preview")
  @Operation(summary = "Generate preview prompt for one outfit",
      description = "Preview language follows current Accept-Language (zh/en/ko).")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Preview generated or degraded for this outfit"),
      @ApiResponse(responseCode = "400", description = "Task not succeeded or outfit references invalid"),
      @ApiResponse(responseCode = "404", description = "Task or outfit not found")
  })
  public Result<MatchTaskResultResponse> generateOutfitPreview(
      @Parameter(description = "Task ID", required = true) @PathVariable String taskId,
      @Parameter(description = "Outfit number in task result", required = true) @PathVariable int outfitNo,
      @Parameter(description = "Preview language (zh/en/ko), defaults to en")
      @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage
  ) {
    return Result.ok(matchTaskService.generateOutfitPreview(taskId, outfitNo, acceptLanguage));
  }

  @GetMapping
  @Operation(summary = "List tasks")
  public Result<PageResponse<MatchTaskSummaryResponse>> listTasks(
      @Parameter(description = "Optional member ID filter") @RequestParam(required = false) Long memberId,
      @Parameter(description = "Page number, starts from 0") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size
  ) {
    return Result.ok(matchTaskService.listTasks(memberId, page, size));
  }

  @GetMapping("/{taskId}/events")
  @Operation(
      summary = "Subscribe task events via SSE",
      description = "Event types: task_started, task_progress, task_completed, task_failed"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "SSE stream opened",
          content = @Content(schema = @Schema(type = "string"))),
      @ApiResponse(responseCode = "404", description = "Task not found")
  })
  public SseEmitter stream(
      @Parameter(description = "Task ID", required = true) @PathVariable String taskId
  ) {
    // Ensure task exists before opening SSE stream.
    matchTaskService.getTask(taskId);
    return sseService.subscribe(taskId);
  }
}
