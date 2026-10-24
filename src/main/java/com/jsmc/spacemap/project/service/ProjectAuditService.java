package com.jsmc.spacemap.project.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jsmc.spacemap.project.dto.response.ProjectAuditLogResponse;
import com.jsmc.spacemap.project.entity.MapProject;
import com.jsmc.spacemap.project.entity.ProjectAuditLog;
import com.jsmc.spacemap.project.entity.User;
import com.jsmc.spacemap.project.mapper.ProjectAuditLogMapper;
import com.jsmc.spacemap.project.repository.ProjectAuditLogRepository;
import com.jsmc.spacemap.project.repository.UserRepository;
import com.jsmc.spacemap.project.service.exception.ResourceNotFoundException;

@Service
public class ProjectAuditService {
    private final ProjectAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ProjectPermissionService permissionService;
    private final ObjectMapper objectMapper;

    public ProjectAuditService(
            ProjectAuditLogRepository auditLogRepository,
            UserRepository userRepository,
            ProjectPermissionService permissionService,
            ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void log(UUID actorUserId, MapProject project, String action, String entityType, UUID entityId, JsonNode details) {
        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        ProjectAuditLog entry = new ProjectAuditLog();
        entry.setProject(project);
        entry.setActor(actor);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDetails(details);
        auditLogRepository.save(entry);
    }

    @Transactional
    public void log(UUID actorUserId, MapProject project, String action, String entityType, UUID entityId) {
        log(actorUserId, project, action, entityType, entityId, null);
    }

    public ObjectNode details(String key, Object value) {
        ObjectNode node = objectMapper.createObjectNode();
        if (value != null) {
            node.putPOJO(key, value);
        }
        return node;
    }

    @Transactional(readOnly = true)
    public Page<ProjectAuditLogResponse> list(UUID userId, UUID projectId, int page, int size) {
        permissionService.requireViewAudit(userId, projectId);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        return auditLogRepository.findByProjectIdOrderByCreatedAtDesc(projectId, PageRequest.of(safePage, safeSize))
                .map(ProjectAuditLogMapper::toResponse);
    }
}
