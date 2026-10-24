package com.jsmc.spacemap.project.dto.request;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record HotspotUpdateRequest(
        UUID targetPanoramaId,
        UUID placeId,
        BigDecimal yaw,
        BigDecimal pitch,
        @Size(max = 255) String label
) {
}
