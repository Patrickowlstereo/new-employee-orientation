package com.gmnl.orientation.stats;

/** 单个小岛的完成统计：完成该小岛全部必修文档的学员数与占比。 */
public record IslandCompletionDto(Long islandId, String islandName, String institutionName,
                                  long completedUsers, long totalLearners, int completionPct) {
}
