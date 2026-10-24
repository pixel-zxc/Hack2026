package com.jsmc.spacemap.project.mapper;

import com.jsmc.spacemap.project.dto.response.RouteNodeResponse;
import com.jsmc.spacemap.project.entity.RouteNode;

public final class RouteNodeMapper {
    private RouteNodeMapper() { }

    public static RouteNodeResponse toResponse(RouteNode node) {
        if (node == null) return null;
        return new RouteNodeResponse(node.getId(), node.getFloor().getId(), node.getPlace() == null ? null : node.getPlace().getId(), node.getX(), node.getY(), node.getNodeType(), node.getAccessible(), node.getCreatedAt());
    }
}
