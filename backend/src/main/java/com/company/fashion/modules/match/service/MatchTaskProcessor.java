package com.company.fashion.modules.match.service;

import com.company.fashion.modules.clothing.entity.Clothing;
import com.company.fashion.modules.clothing.service.ClothingService;
import com.company.fashion.modules.match.ai.AiLanguage;
import com.company.fashion.modules.match.dto.MatchResultItemResponse;
import com.company.fashion.modules.match.dto.OutfitRecommendationResponse;
import com.company.fashion.modules.match.dto.TaskStatus;
import com.company.fashion.modules.match.entity.MatchRecord;
import com.company.fashion.modules.match.entity.MatchRecordStatus;
import com.company.fashion.modules.match.entity.MatchTask;
import com.company.fashion.modules.match.repository.MatchRecordRepository;
import com.company.fashion.modules.match.repository.MatchTaskRepository;
import com.company.fashion.modules.match.sse.SseService;
import com.company.fashion.modules.match.strategy.RecommendationService;
import com.company.fashion.modules.member.entity.Member;
import com.company.fashion.modules.member.service.MemberService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class MatchTaskProcessor {

  private static final Logger log = LoggerFactory.getLogger(MatchTaskProcessor.class);
  private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;

  private final MatchTaskRepository matchTaskRepository;
  private final MatchRecordRepository matchRecordRepository;
  private final MemberService memberService;
  private final ClothingService clothingService;
  private final RecommendationService recommendationService;
  private final SseService sseService;
  private final ObjectMapper objectMapper;

  public MatchTaskProcessor(
      MatchTaskRepository matchTaskRepository,
      MatchRecordRepository matchRecordRepository,
      MemberService memberService,
      ClothingService clothingService,
      RecommendationService recommendationService,
      SseService sseService,
      ObjectMapper objectMapper
  ) {
    this.matchTaskRepository = matchTaskRepository;
    this.matchRecordRepository = matchRecordRepository;
    this.memberService = memberService;
    this.clothingService = clothingService;
    this.recommendationService = recommendationService;
    this.sseService = sseService;
    this.objectMapper = objectMapper;
  }

  @Async("matchExecutor")
  public void processAsync(String taskId) {
    try {
      updateStatus(taskId, TaskStatus.RUNNING, null, null, null, null);
      sseService.publish(taskId, "task_started", Map.of("taskId", taskId, "status", TaskStatus.RUNNING.name()));

      MatchTask task = getTask(taskId);
      Member member = memberService.getActiveEntity(task.getMemberId());
      List<Long> candidateIds = fromJson(task.getCandidateClothingIdsJson());
      List<Clothing> candidates = clothingService.findActiveByIds(candidateIds);
      if (candidates.isEmpty()) {
        throw new IllegalStateException("No valid clothing candidates available");
      }

      Thread.sleep(300);
      sseService.publish(taskId, "task_progress", Map.of("taskId", taskId, "progress", 45));

      List<MatchRecord> history = matchRecordRepository.findTop10ByMemberIdOrderByPerformanceScoreDesc(task.getMemberId());
      RecommendationService.RecommendationOutput output = recommendationService.recommend(
          member,
          candidates,
          history,
          task.getScene(),
          AiLanguage.fromCode(task.getLanguage())
      );
      List<OutfitRecommendationResponse> outfits = output.outfits();
      List<MatchResultItemResponse> flatResult = flattenOutfits(outfits);

      Thread.sleep(300);
      sseService.publish(taskId, "task_progress", Map.of("taskId", taskId, "progress", 85));

      persistRecords(task.getMemberId(), outfits);
      updateStatus(
          taskId,
          TaskStatus.SUCCEEDED,
          output.strategyName(),
          toJson(outfits),
          null,
          output.warning()
      );

      Map<String, Object> completedPayload = new HashMap<>();
      completedPayload.put("taskId", taskId);
      completedPayload.put("status", TaskStatus.SUCCEEDED.name());
      completedPayload.put("strategy", output.strategyName());
      completedPayload.put("outfits", outfits);
      completedPayload.put("result", flatResult);
      completedPayload.put("preview", null);
      completedPayload.put("warning", output.warning());
      sseService.publish(taskId, "task_completed", completedPayload);
    } catch (Throwable ex) {
      handleFailure(taskId, ex);
    }
  }

  private List<MatchResultItemResponse> flattenOutfits(List<OutfitRecommendationResponse> outfits) {
    List<MatchResultItemResponse> result = new ArrayList<>();
    for (OutfitRecommendationResponse outfit : outfits) {
      String reason = outfit.reason() == null ? "AI outfit recommendation" : outfit.reason();
      result.add(new MatchResultItemResponse(
          outfit.topClothingId(),
          "Outfit #" + outfit.outfitNo() + " TOP: " + reason,
          outfit.score()
      ));
      result.add(new MatchResultItemResponse(
          outfit.bottomClothingId(),
          "Outfit #" + outfit.outfitNo() + " BOTTOM: " + reason,
          outfit.score()
      ));
    }
    return result;
  }

  private void persistRecords(Long memberId, List<OutfitRecommendationResponse> outfits) {
    List<MatchResultItemResponse> flattened = flattenOutfits(outfits);
    List<MatchRecord> records = flattened.stream().map(item -> {
      MatchRecord record = new MatchRecord();
      record.setMemberId(memberId);
      record.setClothingId(item.clothingId());
      record.setStatus(MatchRecordStatus.DRAFT);
      record.setPerformanceScore(item.score());
      return record;
    }).toList();
    matchRecordRepository.saveAll(records);
  }

  private void updateStatus(
      String taskId,
      TaskStatus status,
      String strategyName,
      String resultJson,
      String previewJson,
      String errorMessage
  ) {
    MatchTask task = getTask(taskId);
    task.setStatus(status);
    if (strategyName != null) {
      task.setStrategyName(strategyName);
    }
    if (resultJson != null) {
      task.setResultJson(resultJson);
    }
    if (previewJson != null) {
      task.setPreviewJson(previewJson);
    }
    task.setErrorMessage(truncateErrorMessage(errorMessage));
    matchTaskRepository.save(task);
  }

  private void handleFailure(String taskId, Throwable ex) {
    String errorMessage = extractErrorMessage(ex);

    try {
      updateStatus(taskId, TaskStatus.FAILED, null, null, null, errorMessage);
    } catch (Throwable statusEx) {
      log.error("Failed to persist FAILED status for task {}: {}", taskId, statusEx.getMessage(), statusEx);
      try {
        matchTaskRepository.updateStatusAndError(taskId, TaskStatus.FAILED, errorMessage);
      } catch (Throwable fallbackEx) {
        log.error("Fallback FAILED status update also failed for task {}: {}", taskId, fallbackEx.getMessage(), fallbackEx);
      }
    }

    try {
      sseService.publish(taskId, "task_failed", Map.of(
          "taskId", taskId,
          "status", TaskStatus.FAILED.name(),
          "error", errorMessage
      ));
    } catch (Throwable publishEx) {
      log.warn("Failed to publish task_failed SSE for task {}: {}", taskId, publishEx.getMessage());
    }
  }

  private MatchTask getTask(String taskId) {
    return matchTaskRepository.findById(taskId)
        .orElseThrow(() -> new IllegalStateException("Task not found: " + taskId));
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to serialize task data", ex);
    }
  }

  private List<Long> fromJson(String value) {
    try {
      return objectMapper.readValue(value, new TypeReference<>() {
      });
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to parse candidate ids", ex);
    }
  }

  private String extractErrorMessage(Throwable ex) {
    if (ex == null) {
      return "Unknown task processing error";
    }
    String message = ex.getMessage();
    if (message == null || message.isBlank()) {
      message = ex.getClass().getSimpleName();
    }
    return truncateErrorMessage(message);
  }

  private String truncateErrorMessage(String value) {
    if (value == null || value.isBlank()) {
      return value;
    }
    if (value.length() <= MAX_ERROR_MESSAGE_LENGTH) {
      return value;
    }
    return value.substring(0, MAX_ERROR_MESSAGE_LENGTH);
  }
}
