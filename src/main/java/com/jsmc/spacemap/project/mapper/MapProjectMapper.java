package com.jsmc.spacemap.project.mapper;

import com.jsmc.spacemap.project.dto.response.MapProjectResponse;
import com.jsmc.spacemap.project.entity.MapProject;

public final class MapProjectMapper {
    private MapProjectMapper() { }

    public static MapProjectResponse toResponse(MapProject project) {
        if (project == null) return null;
        return new MapProjectResponse(project.getId(), project.getOrganization().getId(), project.getTitle(), project.getDescription(), project.getCoverAsset() == null ? null : project.getCoverAsset().getId(), project.getStatus() == null ? null : project.getStatus().name(), project.getPublicSlug(), project.getCreatedAt(), project.getUpdatedAt());
    }
}
