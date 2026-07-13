package com.gmnl.orientation.content;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 文档行新增/编辑请求（文件上传另走 /upload）。required/order/active 为 null 时取默认。 */
public record DocUpsertRequest(@NotBlank String title, @Schema(nullable = true) String category,
                               @NotNull Long institutionId, @NotNull Long islandId,
                               Boolean required, @Schema(nullable = true) Integer order, Boolean active) {
}
