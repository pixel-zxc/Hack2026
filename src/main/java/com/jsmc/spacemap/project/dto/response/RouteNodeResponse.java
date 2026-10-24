package com.jsmc.spacemap.project.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RouteNodeResponse(
        UUID id,
        UUID floorId,
        UUID placeId,
        BigDecimal x,
        BigDecimal y,
        String nodeType,
        Boolean accessible,
        Instant createdAt
) {
}
