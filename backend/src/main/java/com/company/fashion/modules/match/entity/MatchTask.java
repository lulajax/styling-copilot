package com.company.fashion.modules.match.entity;

import com.company.fashion.modules.match.dto.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Persistent task aggregate for asynchronous match execution.
 *
 * <p>This table stores candidate inputs, strategy name, status transitions, and final result JSON.</p>
 */
@Entity
@Table(name = "match_task")
public class MatchTask {

  @Id
  @Column(length = 64)
  private String id;

  @Column(name = "member_id", nullable = false)
  private Long memberId;

  @Column(name = "operator_username", nullable = false)
  private String operatorUsername;

  @Column(name = "scene")
  private String scene;

  @Column(name = "language", length = 8)
  private String language;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TaskStatus status;

  @Column(name = "strategy_name")
  private String strategyName;

  @Column(name = "candidate_clothing_ids_json", columnDefinition = "text", nullable = false)
  // Candidate clothing IDs after dedup filtering.
  private String candidateClothingIdsJson;

  @Column(name = "result_json", columnDefinition = "text")
  // Serialized list of OutfitRecommendationResponse.
  private String resultJson;

  @Column(name = "preview_json", columnDefinition = "text")
  // Serialized OutfitPreviewResponse.
  private String previewJson;

  @Column(name = "error_message", length = 1000)
  private String errorMessage;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  public void prePersist() {
    LocalDateTime now = LocalDateTime.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PreUpdate
  public void preUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Long getMemberId() {
    return memberId;
  }

  public void setMemberId(Long memberId) {
    this.memberId = memberId;
  }

  public String getOperatorUsername() {
    return operatorUsername;
  }

  public void setOperatorUsername(String operatorUsername) {
    this.operatorUsername = operatorUsername;
  }

  public String getScene() {
    return scene;
  }

  public void setScene(String scene) {
    this.scene = scene;
  }

  public TaskStatus getStatus() {
    return status;
  }

  public void setStatus(TaskStatus status) {
    this.status = status;
  }

  public String getStrategyName() {
    return strategyName;
  }

  public void setStrategyName(String strategyName) {
    this.strategyName = strategyName;
  }

  public String getCandidateClothingIdsJson() {
    return candidateClothingIdsJson;
  }

  public void setCandidateClothingIdsJson(String candidateClothingIdsJson) {
    this.candidateClothingIdsJson = candidateClothingIdsJson;
  }

  public String getResultJson() {
    return resultJson;
  }

  public void setResultJson(String resultJson) {
    this.resultJson = resultJson;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getPreviewJson() {
    return previewJson;
  }

  public void setPreviewJson(String previewJson) {
    this.previewJson = previewJson;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
