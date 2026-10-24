package com.jsmc.spacemap.project.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PathResponse(
        UUID fromNodeId,
        UUID toNodeId,
        BigDecimal totalCost,
        int stepCount,
        boolean found,
        List<UUID> nodeIds,
        List<UUID> edgeIds,
        List<PathStepResponse> steps
) {
}
