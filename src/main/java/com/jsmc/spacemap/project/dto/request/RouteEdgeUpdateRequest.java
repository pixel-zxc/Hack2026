package com.jsmc.spacemap.project.dto.request;

import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record RouteEdgeUpdateRequest(
        @Positive BigDecimal cost,
        Boolean bidirectional,
        String edgeType
) {
}
