package com.gmnl.orientation.stats;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** 单个学员的学习统计：必修完成数/完成率/最后学习时间/小岛完成数。 */
public record UserStatsDto(Long userId, String name, String username,
                           int requiredTotal, int requiredCompleted, int completionPct,
                           @Schema(nullable = true) Instant lastReadAt, int islandsCompleted, int islandsTotal) {
}
