package com.jsmc.spacemap.project.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record HotspotCreateRequest(
        UUID targetPanoramaId,
        UUID placeId,
        @NotNull BigDecimal yaw,
        @NotNull BigDecimal pitch,
        @Size(max = 255) String label
) {
}
