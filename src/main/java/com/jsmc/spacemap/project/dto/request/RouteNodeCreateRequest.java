package com.jsmc.spacemap.project.dto.request;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record RouteNodeCreateRequest(
        @NotNull UUID floorId,
        UUID placeId,
        @NotNull BigDecimal x,
        @NotNull BigDecimal y,
        String nodeType,
        Boolean accessible
) {
}
