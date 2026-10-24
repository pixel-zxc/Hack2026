package com.jsmc.spacemap.project.mapper;

import com.jsmc.spacemap.project.dto.response.ProjectAuditLogResponse;
import com.jsmc.spacemap.project.entity.ProjectAuditLog;

public final class ProjectAuditLogMapper {
    private ProjectAuditLogMapper() { }

    public static ProjectAuditLogResponse toResponse(ProjectAuditLog log) {
        if (log == null) return null;
        return new ProjectAuditLogResponse(
                log.getId(),
                log.getProject().getId(),
                log.getActor().getId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDetails(),
                log.getCreatedAt()
        );
    }
}
