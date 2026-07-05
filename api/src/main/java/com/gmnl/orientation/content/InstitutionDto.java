package com.gmnl.orientation.content;

import java.util.List;

public record InstitutionDto(Long id, String key, String name, Integer order, List<IslandDto> islands) {}
