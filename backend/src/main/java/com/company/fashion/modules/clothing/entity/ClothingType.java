package com.company.fashion.modules.clothing.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Clothing category type used for operation display")
public enum ClothingType {
  TOP,
  BOTTOM,
  ONE_PIECE,
  SET
}

