package com.jsmc.spacemap.project.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AssetResponse(
        UUID id,
        UUID ownerId,
        String contentType,
        Long sizeBytes,
        String status,
        String url,
        Instant createdAt
) {
}
