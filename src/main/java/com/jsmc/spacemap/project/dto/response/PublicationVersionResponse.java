package com.jsmc.spacemap.project.dto.response;

import java.time.Instant;
import java.util.UUID;

public record PublicationVersionResponse(
        UUID id,
        UUID projectId,
        int version,
        Instant publishedAt
) {
}
