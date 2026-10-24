package com.jsmc.spacemap.project.dto.response;

import java.util.List;
import java.util.UUID;

public record VirtualTourResponse(
        UUID projectId,
        UUID startPanoramaId,
        List<PanoramaDetailResponse> panoramas
) {
}
