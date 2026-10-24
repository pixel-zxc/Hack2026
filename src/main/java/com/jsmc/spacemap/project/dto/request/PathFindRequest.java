package com.jsmc.spacemap.project.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PathFindRequest(
        @NotNull UUID fromNodeId,
        @NotNull UUID toNodeId,
        Boolean accessibleOnly,
        Boolean preferElevator,
        Boolean avoidStairs
) {
}
