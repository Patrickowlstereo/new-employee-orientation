package com.gmnl.orientation.progress;

public record IslandStateViewDto(Long islandId, IslandStatus status,
                                 Integer completedCount, Integer totalCount) {}
