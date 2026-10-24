package com.jsmc.spacemap.project.dto.response;

import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        UUID ownerId,
        Instant createdAt,
        Instant updatedAt
) {
}
