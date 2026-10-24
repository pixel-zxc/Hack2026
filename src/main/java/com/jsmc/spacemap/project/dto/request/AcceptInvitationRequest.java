package com.jsmc.spacemap.project.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AcceptInvitationRequest(
        @NotBlank String token
) {
}
