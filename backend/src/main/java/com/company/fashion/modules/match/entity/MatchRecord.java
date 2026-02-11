package com.company.fashion.modules.match.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Historical match record used for dedup filtering and performance analysis.
 */
@Entity
@Table(name = "match_record")
public class MatchRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "member_id", nullable = false)
  private Long memberId;

  @Column(name = "clothing_id", nullable = false)
  private Long clothingId;

  @Column(name = "broadcast_date")
  private LocalDateTime broadcastDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MatchRecordStatus status;

  @Column(name = "performance_score")
  private Integer performanceScore;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @PrePersist
  public void prePersist() {
    this.createdAt = LocalDateTime.now();
  }

  public Long getId() {
    return id;
  }

  public Long getMemberId() {
    return memberId;
  }

  public void setMemberId(Long memberId) {
    this.memberId = memberId;
  }

  public Long getClothingId() {
    return clothingId;
  }

  public void setClothingId(Long clothingId) {
    this.clothingId = clothingId;
  }

  public LocalDateTime getBroadcastDate() {
    return broadcastDate;
  }

  public void setBroadcastDate(LocalDateTime broadcastDate) {
    this.broadcastDate = broadcastDate;
  }

  public MatchRecordStatus getStatus() {
    return status;
  }

  public void setStatus(MatchRecordStatus status) {
    this.status = status;
  }

  public Integer getPerformanceScore() {
    return performanceScore;
  }

  public void setPerformanceScore(Integer performanceScore) {
    this.performanceScore = performanceScore;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
