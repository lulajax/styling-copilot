package com.company.fashion.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Unified API response wrapper")
public record Result<T>(int code, String message, T data) {

  public static <T> Result<T> ok(T data) {
    return new Result<>(0, "OK", data);
  }

  public static Result<Void> ok() {
    return new Result<>(0, "OK", null);
  }

  public static <T> Result<T> error(int code, String message) {
    return new Result<>(code, message, null);
  }
}
