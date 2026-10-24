package com.jsmc.spacemap.project.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record PanoramaCreateRequest(
        @NotNull UUID floorId,
        UUID placeId,
        @NotNull UUID assetId,
        @Size(max = 255) String name,
        BigDecimal initialYaw,
        BigDecimal initialPitch,
        BigDecimal x,
        BigDecimal y
) {
}
