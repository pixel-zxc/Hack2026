package com.jsmc.spacemap.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record FloorCreateRequest(
        @NotBlank @Size(max = 255) String name,
        Integer sortOrder,
        UUID planAssetId,
        @Positive BigDecimal width,
        @Positive BigDecimal height
) {
}
