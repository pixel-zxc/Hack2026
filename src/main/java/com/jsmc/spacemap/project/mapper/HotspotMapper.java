package com.jsmc.spacemap.project.mapper;

import com.jsmc.spacemap.project.dto.response.HotspotResponse;
import com.jsmc.spacemap.project.entity.HotSpot;

public final class HotspotMapper {
    private HotspotMapper() { }

    public static HotspotResponse toResponse(HotSpot hotspot) {
        if (hotspot == null) return null;
        return new HotspotResponse(hotspot.getId(), hotspot.getPanorama().getId(), hotspot.getTargetPanorama() == null ? null : hotspot.getTargetPanorama().getId(), hotspot.getPlace() == null ? null : hotspot.getPlace().getId(), hotspot.getYaw(), hotspot.getPitch(), hotspot.getLabel(), hotspot.getCreatedAt());
    }
}
