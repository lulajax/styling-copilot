package com.company.fashion.modules.match.service;

import com.company.fashion.common.api.PageResponse;
import com.company.fashion.common.exception.BusinessException;
import com.company.fashion.modules.clothing.entity.Clothing;
import com.company.fashion.modules.clothing.service.ClothingService;
import com.company.fashion.modules.match.ai.AiLanguage;
import com.company.fashion.modules.match.ai.MatchLocaleResolver;
import com.company.fashion.modules.match.dto.CreateMatchTaskRequest;
import com.company.fashion.modules.match.dto.CreateMatchTaskResponse;
import com.company.fashion.modules.match.dto.CreateManualHistoryRequest;
import com.company.fashion.modules.match.dto.MatchHistoryItemResponse;
import com.company.fashion.modules.match.dto.MatchHistoryResponse;
import com.company.fashion.modules.match.dto.MatchResultItemResponse;
import com.company.fashion.modules.match.dto.MatchTaskResultResponse;
import com.company.fashion.modules.match.dto.MatchTaskSummaryResponse;
import com.company.fashion.modules.match.dto.OutfitRecommendationResponse;
import com.company.fashion.modules.match.dto.OutfitPreviewResponse;
import com.company.fashion.modules.match.dto.TaskStatus;
import com.company.fashion.modules.match.dto.UpdateMatchRecordStatusRequest;
import com.company.fashion.modules.match.entity.MatchRecord;
import com.company.fashion.modules.match.entity.MatchRecordStatus;
import com.company.fashion.modules.match.entity.MatchTask;
import com.company.fashion.modules.match.repository.MatchRecordRepository;
import com.company.fashion.modules.match.repository.MatchTaskRepository;
import com.company.fashion.modules.member.entity.Member;
import com.company.fashion.modules.member.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchTaskService {

  private final MemberService memberService;
  private final ClothingService clothingService;
  private final MatchRecordRepository matchRecordRepository;
  private final MatchTaskRepository matchTaskRepository;
  private final MatchTaskProcessor matchTaskProcessor;
  private final TaskRateLimitService taskRateLimitService;
  private final OutfitPreviewService outfitPreviewService;
  private final MatchLocaleResolver matchLocaleResolver;
  private final ObjectMapper objectMapper;

  public MatchTaskService(
      MemberService memberService,
      ClothingService clothingService,
      MatchRecordRepository matchRecordRepository,
      MatchTaskRepository matchTaskRepository,
      MatchTaskProcessor matchTaskProcessor,
      TaskRateLimitService taskRateLimitService,
      OutfitPreviewService outfitPreviewService,
      MatchLocaleResolver matchLocaleResolver,
      ObjectMapper objectMapper
  ) {
    this.memberService = memberService;
    this.clothingService = clothingService;
    this.matchRecordRepository = matchRecordRepository;
    this.matchTaskRepository = matchTaskRepository;
    this.matchTaskProcessor = matchTaskProcessor;
    this.taskRateLimitService = taskRateLimitService;
    this.outfitPreviewService = outfitPreviewService;
    this.matchLocaleResolver = matchLocaleResolver;
    this.objectMapper = objectMapper;
  }

  public CreateMatchTaskResponse createTask(
      CreateMatchTaskRequest request,
      String operatorUsername,
      String acceptLanguage
  ) {
    taskRateLimitService.assertAllowed(operatorUsername);
    memberService.getActiveEntity(request.memberId());

    List<Long> requestClothingIds = request.clothingIds().stream().distinct().toList();
    List<Clothing> onShelf = clothingService.findOnShelfByIds(requestClothingIds);
    if (onShelf.isEmpty()) {
      throw new BusinessException(400, "No ON_SHELF clothing found in request");
    }

    LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
    List<MatchRecord> duplicates = matchRecordRepository.findRecentDuplicates(
        request.memberId(),
        onShelf.stream().map(Clothing::getId).toList(),
        MatchRecordStatus.BROADCASTED,
        sevenDaysAgo
    );

    // SQL + code dual-filter to avoid stale cache / concurrent write edge cases.
    Set<Long> duplicateIds = duplicates.stream().map(MatchRecord::getClothingId).collect(Collectors.toSet());
    List<Long> filtered = onShelf.stream().map(Clothing::getId).filter(id -> !duplicateIds.contains(id)).toList();

    if (filtered.isEmpty()) {
      throw new BusinessException(400, "All clothing candidates were filtered by 7-day worn history rule");
    }

    MatchTask task = new MatchTask();
    AiLanguage language = matchLocaleResolver.resolve(acceptLanguage);
    task.setId(UUID.randomUUID().toString());
    task.setMemberId(request.memberId());
    task.setOperatorUsername(operatorUsername);
    task.setScene(request.scene());
    task.setLanguage(language.code());
    task.setStatus(TaskStatus.QUEUED);
    task.setCandidateClothingIdsJson(toJson(filtered));

    matchTaskRepository.save(task);
    matchTaskProcessor.processAsync(task.getId());
    return new CreateMatchTaskResponse(task.getId(), TaskStatus.QUEUED);
  }

  @Transactional(readOnly = true)
  public MatchTaskResultResponse getTask(String taskId) {
    MatchTask task = getTaskEntity(taskId);
    List<OutfitRecommendationResponse> outfits = parseOutfits(task.getResultJson());
    return toTaskResult(task, outfits);
  }

  @Transactional
  public MatchTaskResultResponse generateOutfitPreview(String taskId, int outfitNo, String acceptLanguage) {
    MatchTask task = getTaskEntity(taskId);
    if (task.getStatus() != TaskStatus.SUCCEEDED) {
      throw new BusinessException(400, "Preview can only be generated after task succeeded");
    }

    List<OutfitRecommendationResponse> outfits = parseOutfits(task.getResultJson());
    if (outfits.isEmpty()) {
      throw new BusinessException(400, "No outfit recommendation result found for this task");
    }

    OutfitRecommendationResponse target = outfits.stream()
        .filter(item -> item.outfitNo() == outfitNo)
        .findFirst()
        .orElseThrow(() -> new BusinessException(404, "Outfit not found in task result"));

    Member member = memberService.getActiveEntity(task.getMemberId());
    Map<Long, Clothing> clothingMap = resolveClothingMap(outfits);
    Clothing top = clothingMap.get(target.topClothingId());
    Clothing bottom = clothingMap.get(target.bottomClothingId());
    if (top == null || bottom == null) {
      throw new BusinessException(400, "Outfit clothing references are invalid or not active");
    }

    OutfitPreviewResponse generatedPreview;
    String generatedWarning;
    try {
      AiLanguage language = matchLocaleResolver.resolve(acceptLanguage);
      OutfitPreviewService.OutfitPreviewDecision decision =
          outfitPreviewService.generate(member, List.of(top, bottom), task.getScene(), language);
      generatedPreview = decision.preview();
      generatedWarning = formatOutfitWarning(target.outfitNo(), decision.warning());
    } catch (Throwable ex) {
      generatedPreview = null;
      generatedWarning = "Preview skipped for outfit #" + target.outfitNo() + ": " + extractErrorMessage(ex);
    }

    final OutfitPreviewResponse preview = generatedPreview;
    final String warning = generatedWarning;

    List<OutfitRecommendationResponse> updatedOutfits = outfits.stream()
        .map(item -> item.outfitNo() == target.outfitNo()
            ? new OutfitRecommendationResponse(
                item.outfitNo(),
                item.topClothingId(),
                item.bottomClothingId(),
                item.score(),
                item.reason(),
                preview,
                warning
            )
            : item)
        .toList();

    task.setResultJson(toJson(updatedOutfits));
    OutfitPreviewResponse firstPreview = updatedOutfits.isEmpty() ? null : updatedOutfits.getFirst().preview();
    task.setPreviewJson(firstPreview == null ? null : toJson(firstPreview));
    task.setErrorMessage(mergeWarnings(task.getErrorMessage(), collectOutfitWarnings(updatedOutfits)));
    MatchTask saved = matchTaskRepository.save(task);
    return toTaskResult(saved, updatedOutfits);
  }

  @Transactional(readOnly = true)
  public PageResponse<MatchTaskSummaryResponse> listTasks(Long memberId, int page, int size) {
    Page<MatchTask> taskPage = memberId == null
        ? matchTaskRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
        : matchTaskRepository.findByMemberIdOrderByCreatedAtDesc(memberId, PageRequest.of(page, size));

    List<MatchTaskSummaryResponse> items = taskPage.getContent().stream()
        .map(task -> new MatchTaskSummaryResponse(
            task.getId(),
            task.getMemberId(),
            task.getScene(),
            task.getStatus(),
            task.getStrategyName(),
            task.getCreatedAt()
        ))
        .toList();

    return new PageResponse<>(items, taskPage.getTotalElements(), page, size);
  }

  @Transactional(readOnly = true)
  public MatchHistoryResponse getHistory(Long memberId, int limit) {
    String memberName = memberService.getActiveEntity(memberId).getName();
    List<MatchRecord> historyRecords = matchRecordRepository
        .findByMemberIdOrderByCreatedAtDesc(memberId, PageRequest.of(0, limit))
        .stream()
        .toList();
    Map<Long, String> clothingNames = resolveClothingNames(historyRecords.stream().map(MatchRecord::getClothingId).toList());
    List<MatchHistoryItemResponse> records = historyRecords.stream()
        .map(record -> toHistoryItem(record, memberName, clothingNames.get(record.getClothingId())))
        .toList();
    return new MatchHistoryResponse(records, matchRecordRepository.countByMemberId(memberId));
  }

  @Transactional
  public MatchHistoryItemResponse createManualHistory(Long memberId, CreateManualHistoryRequest request) {
    String memberName = memberService.getActiveEntity(memberId).getName();
    Clothing clothing = clothingService.getActiveEntity(request.clothingId());

    MatchRecord record = new MatchRecord();
    record.setMemberId(memberId);
    record.setClothingId(request.clothingId());
    record.setStatus(MatchRecordStatus.BROADCASTED);
    record.setPerformanceScore(request.performanceScore());
    record.setBroadcastDate(request.broadcastDate() == null ? LocalDateTime.now() : request.broadcastDate());
    return toHistoryItem(matchRecordRepository.save(record), memberName, clothing.getName());
  }

  @Transactional
  public MatchHistoryItemResponse updateHistoryStatus(
      Long memberId,
      Long recordId,
      UpdateMatchRecordStatusRequest request
  ) {
    String memberName = memberService.getActiveEntity(memberId).getName();
    MatchRecord record = matchRecordRepository.findByIdAndMemberId(recordId, memberId)
        .orElseThrow(() -> new BusinessException(404, "History record not found"));

    record.setStatus(request.status());
    if (request.status() == MatchRecordStatus.BROADCASTED && record.getBroadcastDate() == null) {
      record.setBroadcastDate(LocalDateTime.now());
    } else if (request.status() != MatchRecordStatus.BROADCASTED) {
      // When corrected to "not worn", this record should not participate in worn-history filtering.
      record.setBroadcastDate(null);
    }
    String clothingName = resolveClothingNames(List.of(record.getClothingId())).get(record.getClothingId());
    return toHistoryItem(matchRecordRepository.save(record), memberName, clothingName);
  }

  private MatchTask getTaskEntity(String taskId) {
    return matchTaskRepository.findById(taskId)
        .orElseThrow(() -> new BusinessException(404, "Task not found"));
  }

  private MatchTaskResultResponse toTaskResult(MatchTask task, List<OutfitRecommendationResponse> outfits) {
    List<MatchResultItemResponse> legacyResult = outfits.isEmpty()
        ? parseLegacyResultItems(task.getResultJson())
        : flattenOutfits(outfits);
    OutfitPreviewResponse legacyPreview = outfits.isEmpty()
        ? parsePreview(task.getPreviewJson())
        : outfits.getFirst().preview();
    return new MatchTaskResultResponse(
        task.getId(),
        task.getStatus(),
        task.getStrategyName(),
        outfits,
        legacyResult,
        legacyPreview,
        task.getErrorMessage()
    );
  }

  private Map<Long, Clothing> resolveClothingMap(List<OutfitRecommendationResponse> outfits) {
    Set<Long> ids = outfits.stream()
        .flatMap(item -> java.util.stream.Stream.of(item.topClothingId(), item.bottomClothingId()))
        .filter(id -> id != null)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    if (ids.isEmpty()) {
      return Map.of();
    }
    return clothingService.findActiveByIds(List.copyOf(ids)).stream()
        .collect(Collectors.toMap(Clothing::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
  }

  private Map<Long, String> resolveClothingNames(List<Long> clothingIds) {
    if (clothingIds == null || clothingIds.isEmpty()) {
      return Map.of();
    }
    return clothingService.findActiveByIds(clothingIds).stream()
        .collect(Collectors.toMap(Clothing::getId, Clothing::getName, (left, right) -> left));
  }

  private MatchHistoryItemResponse toHistoryItem(MatchRecord record, String memberName, String clothingName) {
    return new MatchHistoryItemResponse(
        record.getId(),
        record.getMemberId(),
        memberName,
        record.getClothingId(),
        clothingName,
        record.getStatus(),
        record.getPerformanceScore(),
        record.getBroadcastDate()
    );
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to serialize task data", ex);
    }
  }

  private List<OutfitRecommendationResponse> parseOutfits(String value) {
    if (value == null || value.isBlank()) {
      return List.of();
    }
    try {
      List<OutfitRecommendationResponse> parsed = objectMapper.readValue(
          value,
          objectMapper.getTypeFactory().constructCollectionType(List.class, OutfitRecommendationResponse.class)
      );
      return parsed.stream()
          .filter(item -> item != null && item.topClothingId() != null && item.bottomClothingId() != null)
          .toList();
    } catch (Exception ex) {
      return List.of();
    }
  }

  private List<MatchResultItemResponse> parseLegacyResultItems(String value) {
    if (value == null || value.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(
          value,
          objectMapper.getTypeFactory().constructCollectionType(List.class, MatchResultItemResponse.class)
      );
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to deserialize task data", ex);
    }
  }

  private List<MatchResultItemResponse> flattenOutfits(List<OutfitRecommendationResponse> outfits) {
    List<MatchResultItemResponse> flat = new java.util.ArrayList<>();
    for (OutfitRecommendationResponse outfit : outfits) {
      String reason = outfit.reason() == null ? "AI outfit recommendation" : outfit.reason();
      flat.add(new MatchResultItemResponse(
          outfit.topClothingId(),
          "Outfit #" + outfit.outfitNo() + " TOP: " + reason,
          outfit.score()
      ));
      flat.add(new MatchResultItemResponse(
          outfit.bottomClothingId(),
          "Outfit #" + outfit.outfitNo() + " BOTTOM: " + reason,
          outfit.score()
      ));
    }
    return flat;
  }

  private OutfitPreviewResponse parsePreview(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return objectMapper.readValue(value, OutfitPreviewResponse.class);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to deserialize preview data", ex);
    }
  }

  private String formatOutfitWarning(int outfitNo, String warning) {
    if (warning == null || warning.isBlank()) {
      return null;
    }
    String normalized = warning.trim();
    if (normalized.startsWith("Preview skipped for outfit #")) {
      return normalized;
    }
    return "Preview skipped for outfit #" + outfitNo + ": " + normalized;
  }

  private String collectOutfitWarnings(List<OutfitRecommendationResponse> outfits) {
    return outfits.stream()
        .map(OutfitRecommendationResponse::warning)
        .filter(item -> item != null && !item.isBlank())
        .collect(Collectors.joining("; "));
  }

  private String mergeWarnings(String... warnings) {
    LinkedHashSet<String> uniq = new LinkedHashSet<>();
    for (String warning : warnings) {
      if (warning == null || warning.isBlank()) {
        continue;
      }
      String[] tokens = warning.split(";");
      for (String token : tokens) {
        String item = token.trim();
        if (!item.isEmpty()) {
          uniq.add(item);
        }
      }
    }
    if (uniq.isEmpty()) {
      return null;
    }
    return String.join("; ", uniq);
  }

  private String extractErrorMessage(Throwable ex) {
    if (ex == null) {
      return "Unknown preview error";
    }
    String message = ex.getMessage();
    if (message == null || message.isBlank()) {
      message = ex.getClass().getSimpleName();
    }
    return message.length() > 1000 ? message.substring(0, 1000) : message;
  }
}
