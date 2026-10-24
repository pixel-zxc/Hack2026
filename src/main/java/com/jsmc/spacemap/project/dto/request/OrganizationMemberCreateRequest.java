package com.jsmc.spacemap.project.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrganizationMemberCreateRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 50) String role
) {
}
