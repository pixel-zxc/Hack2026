package com.jsmc.spacemap.project.dto.response;

import java.time.Instant;
import java.util.UUID;

public record MapProjectResponse(
        UUID id,
        UUID organizationId,
        String title,
        String description,
        UUID coverAssetId,
        String status,
        String publicSlug,
        Instant createdAt,
        Instant updatedAt
) {
}
