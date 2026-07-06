package com.gmnl.orientation.stats;

import java.util.List;

/** 全员学习统计概览。 */
public record StatsOverviewDto(long totalUsers, long totalLearners, long completedAllRequired,
                               int avgCompletionPct, List<IslandCompletionDto> islands) {
}
