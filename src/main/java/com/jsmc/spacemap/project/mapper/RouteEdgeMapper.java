package com.jsmc.spacemap.project.mapper;

import com.jsmc.spacemap.project.dto.response.RouteEdgeResponse;
import com.jsmc.spacemap.project.entity.RouteEdge;

public final class RouteEdgeMapper {
    private RouteEdgeMapper() { }

    public static RouteEdgeResponse toResponse(RouteEdge edge) {
        if (edge == null) return null;
        return new RouteEdgeResponse(edge.getId(), edge.getFromNode().getId(), edge.getToNode().getId(), edge.getCost(), edge.getBidirectional(), edge.getEdgeType(), edge.getCreatedAt());
    }
}
