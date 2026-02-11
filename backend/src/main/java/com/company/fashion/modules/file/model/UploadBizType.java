package com.company.fashion.modules.file.model;

import com.company.fashion.common.exception.BusinessException;
import com.fasterxml.jackson.annotation.JsonValue;

public enum UploadBizType {
  MEMBER("member"),
  CLOTHING("clothing");

  private final String value;

  UploadBizType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  public static UploadBizType from(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new BusinessException(400, "bizType is required");
    }
    for (UploadBizType type : values()) {
      if (type.value.equalsIgnoreCase(raw.trim())) {
        return type;
      }
    }
    throw new BusinessException(400, "Unsupported bizType: " + raw);
  }
}
