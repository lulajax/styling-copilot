package com.company.fashion.modules.clothing.repository;

import com.company.fashion.modules.clothing.entity.Clothing;
import com.company.fashion.modules.clothing.entity.ClothingStatus;
import com.company.fashion.modules.clothing.entity.ClothingType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ClothingRepository extends JpaRepository<Clothing, Long> {

  Page<Clothing> findByStatusAndDeletedFalse(ClothingStatus status, Pageable pageable);

  List<Clothing> findByIdInAndStatusAndDeletedFalse(List<Long> ids, ClothingStatus status);

  Optional<Clothing> findByIdAndDeletedFalse(Long id);

  Page<Clothing> findAllByDeletedFalse(Pageable pageable);

  List<Clothing> findByIdInAndDeletedFalse(List<Long> ids);

  @Modifying
  @Query("update Clothing c set c.clothingType = :defaultType where c.clothingType is null")
  int backfillNullClothingType(ClothingType defaultType);
}
