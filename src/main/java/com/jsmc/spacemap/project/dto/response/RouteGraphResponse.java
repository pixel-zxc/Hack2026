package com.jsmc.spacemap.project.dto.response;

import java.util.List;

public record RouteGraphResponse(
        java.util.UUID projectId,
        List<RouteNodeResponse> nodes,
        List<RouteEdgeResponse> edges
) {
}
