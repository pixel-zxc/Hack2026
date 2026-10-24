package com.jsmc.spacemap.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrganizationCreateRequest(
        @NotBlank @Size(max = 255) String name
) {
}
