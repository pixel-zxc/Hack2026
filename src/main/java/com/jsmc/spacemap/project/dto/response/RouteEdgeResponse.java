package com.jsmc.spacemap.project.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RouteEdgeResponse(
        UUID id,
        UUID fromNodeId,
        UUID toNodeId,
        BigDecimal cost,
        Boolean bidirectional,
        String edgeType,
        Instant createdAt
) {
}
