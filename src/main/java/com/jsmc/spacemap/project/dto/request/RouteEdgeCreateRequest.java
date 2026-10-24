package com.jsmc.spacemap.project.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record RouteEdgeCreateRequest(
        @NotNull UUID fromNodeId,
        @NotNull UUID toNodeId,
        @Positive BigDecimal cost,
        Boolean bidirectional,
        String edgeType
) {
}
