package com.jsmc.spacemap.project.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FloorResponse(
        UUID id,
        UUID projectId,
        String name,
        Integer sortOrder,
        UUID planAssetId,
        BigDecimal width,
        BigDecimal height,
        Instant createdAt,
        Instant updatedAt
) {
}
