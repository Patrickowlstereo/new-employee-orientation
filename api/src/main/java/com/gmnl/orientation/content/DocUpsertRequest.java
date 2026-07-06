package com.gmnl.orientation.content;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 文档行新增/编辑请求（文件上传另走 /upload）。required/order/active 为 null 时取默认。 */
public record DocUpsertRequest(@NotBlank String title, String category,
                               @NotNull Long institutionId, @NotNull Long islandId,
                               Boolean required, Integer order, Boolean active) {
}
