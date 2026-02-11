package com.company.fashion.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Generic paged response")
public record PageResponse<T>(
    List<T> items,
    long total,
    int page,
    int size
) {
}
