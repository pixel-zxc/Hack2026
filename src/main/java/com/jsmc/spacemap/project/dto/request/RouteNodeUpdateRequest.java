package com.jsmc.spacemap.project.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

public record RouteNodeUpdateRequest(
        UUID placeId,
        BigDecimal x,
        BigDecimal y,
        String nodeType,
        Boolean accessible
) {
}
