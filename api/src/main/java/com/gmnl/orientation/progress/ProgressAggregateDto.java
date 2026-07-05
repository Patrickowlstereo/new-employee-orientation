package com.gmnl.orientation.progress;

import java.util.List;

public record ProgressAggregateDto(List<ProgressItemDto> documents,
                                   List<IslandStateViewDto> islands) {}
