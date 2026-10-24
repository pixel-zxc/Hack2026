package com.jsmc.spacemap.project.mapper;

import com.jsmc.spacemap.project.dto.response.PublicationVersionResponse;
import com.jsmc.spacemap.project.entity.PublicationVersion;

public final class PublicationVersionMapper {
    private PublicationVersionMapper() { }

    public static PublicationVersionResponse toResponse(PublicationVersion v) {
        if (v == null) return null;
        return new PublicationVersionResponse(
                v.getId(),
                v.getProject().getId(),
                v.getVersion(),
                v.getPublishedAt()
        );
    }
}
