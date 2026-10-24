package com.jsmc.spacemap.project.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ProjectMemberResponse(
        UUID id,
        UUID projectId,
        UUID userId,
        String email,
        String displayName,
        String role,
        UUID invitedBy,
        Instant createdAt,
        Instant updatedAt
) {
}
