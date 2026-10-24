package com.jsmc.spacemap.project.mapper;

import com.jsmc.spacemap.project.dto.response.PanoramaResponse;
import com.jsmc.spacemap.project.entity.Panorama;

public final class PanoramaMapper {
    private PanoramaMapper() { }

    public static PanoramaResponse toResponse(Panorama panorama) {
        if (panorama == null) return null;
        return new PanoramaResponse(panorama.getId(), panorama.getPlace() == null ? null : panorama.getPlace().getId(), panorama.getFloor().getId(), panorama.getAsset().getId(), panorama.getName(), panorama.getInitialYaw(), panorama.getInitialPitch(), panorama.getX(), panorama.getY(), panorama.getStatus() == null ? null : panorama.getStatus().name(), panorama.getCreatedAt(), panorama.getUpdatedAt());
    }
}
