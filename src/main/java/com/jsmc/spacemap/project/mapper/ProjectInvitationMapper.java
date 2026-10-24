package com.jsmc.spacemap.project.mapper;

import com.jsmc.spacemap.project.dto.response.ProjectInvitationResponse;
import com.jsmc.spacemap.project.entity.ProjectInvitation;

public final class ProjectInvitationMapper {
    private ProjectInvitationMapper() { }

    public static ProjectInvitationResponse toResponse(ProjectInvitation inv, String plainToken) {
        if (inv == null) return null;
        return new ProjectInvitationResponse(
                inv.getId(),
                inv.getProject().getId(),
                inv.getEmail(),
                inv.getRole().name(),
                inv.getStatus().name(),
                inv.getInvitedBy().getId(),
                inv.getExpiresAt(),
                inv.getCreatedAt(),
                inv.getAcceptedAt(),
                plainToken
        );
    }
}
