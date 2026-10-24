package com.jsmc.spacemap.project.mapper;

import com.jsmc.spacemap.project.dto.response.FloorResponse;
import com.jsmc.spacemap.project.entity.Floor;

public final class FloorMapper {
    private FloorMapper() { }

    public static FloorResponse toResponse(Floor floor) {
        if (floor == null) return null;
        return new FloorResponse(floor.getId(), floor.getProject().getId(), floor.getName(), floor.getSortOrder(), floor.getPlanAsset() == null ? null : floor.getPlanAsset().getId(), floor.getWidth(), floor.getHeight(), floor.getCreatedAt(), floor.getUpdatedAt());
    }
}
