package com.jsmc.spacemap.project.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record ProjectAuditLogResponse(
        UUID id,
        UUID projectId,
        UUID actorUserId,
        String action,
        String entityType,
        UUID entityId,
        JsonNode details,
        Instant createdAt
) {
}
