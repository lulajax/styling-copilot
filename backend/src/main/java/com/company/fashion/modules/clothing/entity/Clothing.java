package com.company.fashion.modules.clothing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Clothing inventory item that participates in match candidate selection.
 *
 * <p>Soft-deleted items stay in historical records but are hidden from new tasks.</p>
 */
@Entity
@Table(name = "clothing")
@SQLDelete(sql = "UPDATE clothing SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
public class Clothing {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(name = "image_url")
  private String imageUrl;

  @Column(name = "style_tags")
  // Comma-separated tags used by recommendation strategies.
  private String styleTags;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ClothingStatus status = ClothingStatus.ON_SHELF;

  @Enumerated(EnumType.STRING)
  @Column(name = "clothing_type")
  private ClothingType clothingType;

  @Column(name = "size_data", columnDefinition = "text")
  // JSON string for clothing size measurements.
  private String sizeData;

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

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public String getStyleTags() {
    return styleTags;
  }

  public void setStyleTags(String styleTags) {
    this.styleTags = styleTags;
  }

  public ClothingStatus getStatus() {
    return status;
  }

  public void setStatus(ClothingStatus status) {
    this.status = status;
  }

  public ClothingType getClothingType() {
    return clothingType;
  }

  public void setClothingType(ClothingType clothingType) {
    this.clothingType = clothingType;
  }

  public String getSizeData() {
    return sizeData;
  }

  public void setSizeData(String sizeData) {
    this.sizeData = sizeData;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }
}
