package com.jsmc.spacemap.project.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PanoramaDetailResponse(
        UUID id,
        UUID placeId,
        UUID floorId,
        UUID assetId,
        String assetUrl,
        String name,
        BigDecimal initialYaw,
        BigDecimal initialPitch,
        BigDecimal x,
        BigDecimal y,
        String status,
        List<HotspotResponse> hotspots,
        Instant createdAt,
        Instant updatedAt
) {
}
