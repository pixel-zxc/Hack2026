package com.jsmc.spacemap.project.dto.request;

import com.jsmc.spacemap.project.entity.ProjectRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProjectInviteRequest(
        @NotBlank @Email String email,
        @NotNull ProjectRole role
) {
}
