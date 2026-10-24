package com.jsmc.spacemap.project.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

public record PublicMapResponse(
        UUID projectId,
        String title,
        String description,
        String publicSlug,
        Integer version,
        JsonNode snapshot
) {
}
