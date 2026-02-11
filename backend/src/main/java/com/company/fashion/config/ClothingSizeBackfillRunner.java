package com.company.fashion.config;

import com.company.fashion.modules.clothing.entity.ClothingType;
import com.company.fashion.modules.clothing.repository.ClothingRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(20)
public class ClothingSizeBackfillRunner implements CommandLineRunner {

  private final ClothingRepository clothingRepository;

  public ClothingSizeBackfillRunner(ClothingRepository clothingRepository) {
    this.clothingRepository = clothingRepository;
  }

  @Override
  @Transactional
  public void run(String... args) {
    clothingRepository.backfillNullClothingType(ClothingType.SET);
  }
}
