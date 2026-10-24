package com.jsmc.spacemap.project.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record HotspotResponse(
        UUID id,
        UUID panoramaId,
        UUID targetPanoramaId,
        UUID placeId,
        BigDecimal yaw,
        BigDecimal pitch,
        String label,
        Instant createdAt
) {
}
