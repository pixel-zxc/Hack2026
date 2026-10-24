package com.jsmc.spacemap.project.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record PathStepResponse(
        int index,
        UUID fromNodeId,
        UUID toNodeId,
        UUID fromFloorId,
        UUID toFloorId,
        String edgeType,
        String nodeType,
        BigDecimal distance,
        String instruction
) {
}
