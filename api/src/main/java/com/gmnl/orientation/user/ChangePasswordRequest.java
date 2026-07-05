package com.gmnl.orientation.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank String oldPassword,
    @NotBlank @Size(min = 8, message = "密码至少 8 位") String newPassword) {}
