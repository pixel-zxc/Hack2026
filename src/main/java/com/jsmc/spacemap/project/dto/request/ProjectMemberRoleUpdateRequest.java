package com.jsmc.spacemap.project.dto.request;

import com.jsmc.spacemap.project.entity.ProjectRole;
import jakarta.validation.constraints.NotNull;

public record ProjectMemberRoleUpdateRequest(
        @NotNull ProjectRole role
) {
}
