package com.jsmc.spacemap.project.mapper;

import com.jsmc.spacemap.project.dto.response.PlaceResponse;
import com.jsmc.spacemap.project.entity.Place;

public final class PlaceMapper {
    private PlaceMapper() { }

    public static PlaceResponse toResponse(Place place) {
        if (place == null) return null;
        return new PlaceResponse(place.getId(), place.getFloor().getId(), place.getName(), place.getCategory(), place.getDescription(), place.getGeometry(), place.getX(), place.getY(), place.getCreatedAt(), place.getUpdatedAt());
    }
}
