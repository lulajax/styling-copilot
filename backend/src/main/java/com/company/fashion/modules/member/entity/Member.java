package com.company.fashion.modules.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * Member profile used by matching.
 *
 * <p>Soft delete is enabled to retain historical analytics data.</p>
 */
@Entity
@Table(name = "member")
@SQLDelete(sql = "UPDATE member SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
public class Member {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(name = "body_data", columnDefinition = "text")
  // JSON string for body profile, e.g. {"height":168,"shape":"X"}.
  private String bodyData;

  @Column(name = "style_tags")
  // Comma-separated tags used by recommendation strategies.
  private String styleTags;

  @Column(name = "photo_url")
  private String photoUrl;

  @Column(nullable = false)
  private boolean deleted = false;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
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

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getBodyData() {
    return bodyData;
  }

  public void setBodyData(String bodyData) {
    this.bodyData = bodyData;
  }

  public String getStyleTags() {
    return styleTags;
  }

  public void setStyleTags(String styleTags) {
    this.styleTags = styleTags;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public String getPhotoUrl() {
    return photoUrl;
  }

  public void setPhotoUrl(String photoUrl) {
    this.photoUrl = photoUrl;
  }
}
