package com.gmnl.orientation.content;

import jakarta.validation.constraints.NotBlank;

/** 机构新增/编辑请求。key 全局唯一。 */
public record InstitutionUpsertRequest(@NotBlank String key, @NotBlank String name, Integer order) {
}
