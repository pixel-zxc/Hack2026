package com.jsmc.spacemap.project.dto.response;

import java.util.List;
import java.util.UUID;

public record TourConnectivityResponse(
        UUID floorId,
        int panoramaCount,
        int edgeCount,
        boolean fullyConnected,
        List<UUID> isolatedPanoramaIds,
        List<Integer> componentSizes
) {
}
