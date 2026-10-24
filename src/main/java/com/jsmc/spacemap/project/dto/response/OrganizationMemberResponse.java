package com.jsmc.spacemap.project.dto.response;

import java.time.Instant;
import java.util.UUID;

public record OrganizationMemberResponse(
        UUID id,
        UUID organizationId,
        UUID userId,
        String email,
        String displayName,
        String role,
        Instant createdAt
) {
}
