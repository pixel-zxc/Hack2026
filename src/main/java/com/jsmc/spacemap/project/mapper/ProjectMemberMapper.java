package com.jsmc.spacemap.project.mapper;

import com.jsmc.spacemap.project.dto.response.ProjectMemberResponse;
import com.jsmc.spacemap.project.entity.ProjectMember;

public final class ProjectMemberMapper {
    private ProjectMemberMapper() { }

    public static ProjectMemberResponse toResponse(ProjectMember m) {
        if (m == null) return null;
        return new ProjectMemberResponse(
                m.getId(),
                m.getProject().getId(),
                m.getUser().getId(),
                m.getUser().getEmail(),
                m.getUser().getDisplayName(),
                m.getRole().name(),
                m.getInvitedBy() == null ? null : m.getInvitedBy().getId(),
                m.getCreatedAt(),
                m.getUpdatedAt()
        );
    }
}
