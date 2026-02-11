package com.company.fashion.modules.clothing.service;

import com.company.fashion.common.api.PageResponse;
import com.company.fashion.common.exception.BusinessException;
import com.company.fashion.modules.clothing.dto.ClothingItemResponse;
import com.company.fashion.modules.clothing.dto.ClothingPageResponse;
import com.company.fashion.modules.clothing.dto.CreateClothingRequest;
import com.company.fashion.modules.clothing.dto.UpdateClothingRequest;
import com.company.fashion.modules.clothing.entity.Clothing;
import com.company.fashion.modules.clothing.entity.ClothingStatus;
import com.company.fashion.modules.clothing.entity.ClothingType;
import com.company.fashion.modules.clothing.repository.ClothingRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClothingService {

  private final ClothingRepository clothingRepository;

  public ClothingService(ClothingRepository clothingRepository) {
    this.clothingRepository = clothingRepository;
  }

  @Transactional
  public ClothingItemResponse create(CreateClothingRequest request) {
    validateWritableType(request.clothingType());
    Clothing clothing = new Clothing();
    clothing.setName(request.name());
    clothing.setImageUrl(request.imageUrl());
    clothing.setStyleTags(request.styleTags());
    clothing.setClothingType(request.clothingType());
    clothing.setStatus(request.status() == null ? ClothingStatus.ON_SHELF : request.status());
    clothing.setSizeData(request.sizeData());
    return toResponse(clothingRepository.save(clothing));
  }

  @Transactional(readOnly = true)
  public ClothingPageResponse list(ClothingStatus status, int page, int size) {
    ClothingStatus finalStatus = status == null ? ClothingStatus.ON_SHELF : status;
    Page<Clothing> clothingPage = clothingRepository.findByStatusAndDeletedFalse(finalStatus, PageRequest.of(page, size));
    List<ClothingItemResponse> items = clothingPage.getContent().stream().map(this::toResponse).toList();
    return new ClothingPageResponse(items, clothingPage.getTotalElements());
  }

  @Transactional(readOnly = true)
  public PageResponse<ClothingItemResponse> listAll(int page, int size) {
    Page<Clothing> clothingPage = clothingRepository.findAllByDeletedFalse(PageRequest.of(page, size));
    return new PageResponse<>(
        clothingPage.getContent().stream().map(this::toResponse).toList(),
        clothingPage.getTotalElements(),
        page,
        size
    );
  }

  @Transactional(readOnly = true)
  public ClothingItemResponse get(Long id) {
    return toResponse(getActiveEntity(id));
  }

  @Transactional
  public ClothingItemResponse update(Long id, UpdateClothingRequest request) {
    validateWritableType(request.clothingType());
    Clothing clothing = getActiveEntity(id);
    if (request.name() != null && !request.name().isBlank()) {
      clothing.setName(request.name());
    }
    if (request.imageUrl() != null) {
      clothing.setImageUrl(request.imageUrl());
    }
    if (request.styleTags() != null) {
      clothing.setStyleTags(request.styleTags());
    }
    clothing.setClothingType(request.clothingType());
    if (request.sizeData() != null) {
      clothing.setSizeData(request.sizeData());
    }
    return toResponse(clothingRepository.save(clothing));
  }

  @Transactional
  public ClothingItemResponse updateStatus(Long id, ClothingStatus status) {
    Clothing clothing = getActiveEntity(id);
    clothing.setStatus(status);
    return toResponse(clothingRepository.save(clothing));
  }

  @Transactional
  public void delete(Long id) {
    Clothing clothing = getActiveEntity(id);
    clothing.setDeleted(true);
    clothingRepository.save(clothing);
  }

  @Transactional(readOnly = true)
  public List<Clothing> findOnShelfByIds(List<Long> ids) {
    return clothingRepository.findByIdInAndStatusAndDeletedFalse(ids, ClothingStatus.ON_SHELF);
  }

  @Transactional(readOnly = true)
  public List<Clothing> findActiveByIds(List<Long> ids) {
    return clothingRepository.findByIdInAndDeletedFalse(ids);
  }

  @Transactional(readOnly = true)
  public Clothing getActiveEntity(Long id) {
    return clothingRepository.findByIdAndDeletedFalse(id)
        .orElseThrow(() -> new BusinessException(404, "Clothing not found"));
  }

  private ClothingItemResponse toResponse(Clothing clothing) {
    return new ClothingItemResponse(
        clothing.getId(),
        clothing.getName(),
        clothing.getImageUrl(),
        clothing.getStyleTags(),
        clothing.getClothingType(),
        clothing.getStatus(),
        clothing.getSizeData()
    );
  }

  private void validateWritableType(ClothingType type) {
    if (type != ClothingType.TOP && type != ClothingType.BOTTOM) {
      throw new BusinessException(400, "clothingType must be TOP or BOTTOM");
    }
  }
}
