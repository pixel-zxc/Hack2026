package com.jsmc.spacemap.project.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PanoramaResponse(
        UUID id,
        UUID placeId,
        UUID floorId,
        UUID assetId,
        String name,
        BigDecimal initialYaw,
        BigDecimal initialPitch,
        BigDecimal x,
        BigDecimal y,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
