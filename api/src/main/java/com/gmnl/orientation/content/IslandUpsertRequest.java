package com.gmnl.orientation.content;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 小岛新增/编辑请求。key 全局唯一，institutionId 指定所属机构。 */
public record IslandUpsertRequest(@NotBlank String key, @NotBlank String name,
                                  @Schema(nullable = true) Integer order,
                                  @NotNull Long institutionId) {
}
