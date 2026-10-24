package com.jsmc.spacemap.project.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record PublishedMapResponse(
        UUID projectId,
        String title,
        String description,
        String publicSlug,
        String status,
        int version,
        Instant publishedAt,
        JsonNode snapshot
) {
}
