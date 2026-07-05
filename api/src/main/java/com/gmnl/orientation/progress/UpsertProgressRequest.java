package com.gmnl.orientation.progress;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpsertProgressRequest(
    @NotNull ProgressStatus status,
    @Min(0) @Max(100) Integer progressPct) {}
