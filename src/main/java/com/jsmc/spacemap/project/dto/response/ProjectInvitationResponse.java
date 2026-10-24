package com.jsmc.spacemap.project.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ProjectInvitationResponse(
        UUID id,
        UUID projectId,
        String email,
        String role,
        String status,
        UUID invitedBy,
        Instant expiresAt,
        Instant createdAt,
        Instant acceptedAt,
        String token
) {
}
