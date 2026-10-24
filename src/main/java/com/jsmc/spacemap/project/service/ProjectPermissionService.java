package com.jsmc.spacemap.project.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jsmc.spacemap.project.entity.MapProject;
import com.jsmc.spacemap.project.entity.ProjectMember;
import com.jsmc.spacemap.project.entity.ProjectRole;
import com.jsmc.spacemap.project.repository.MapProjectRepository;
import com.jsmc.spacemap.project.repository.ProjectMemberRepository;
import com.jsmc.spacemap.project.service.exception.ForbiddenOperationException;
import com.jsmc.spacemap.project.service.exception.ResourceNotFoundException;

@Service
public class ProjectPermissionService {
    private final MapProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;

    public ProjectPermissionService(MapProjectRepository projectRepository, ProjectMemberRepository memberRepository) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public MapProject requireView(UUID userId, UUID projectId) {
        MapProject project = findProject(projectId);
        ProjectRole role = resolveRole(userId, project);
        if (role == null) {
            throw new ForbiddenOperationException("Нет доступа к проекту");
        }
        return project;
    }

    @Transactional(readOnly = true)
    public MapProject requireEdit(UUID userId, UUID projectId) {
        MapProject project = findProject(projectId);
        ProjectRole role = resolveRole(userId, project);
        if (role != ProjectRole.OWNER && role != ProjectRole.EDITOR) {
            throw new ForbiddenOperationException("Недостаточно прав для изменения проекта");
        }
        return project;
    }

    @Transactional(readOnly = true)
    public MapProject requireManageMembers(UUID userId, UUID projectId) {
        MapProject project = findProject(projectId);
        if (resolveRole(userId, project) != ProjectRole.OWNER) {
            throw new ForbiddenOperationException("Только владелец может управлять участниками");
        }
        return project;
    }

    @Transactional(readOnly = true)
    public MapProject requireTransferOwnership(UUID userId, UUID projectId) {
        return requireManageMembers(userId, projectId);
    }

    @Transactional(readOnly = true)
    public MapProject requireDeleteProject(UUID userId, UUID projectId) {
        return requireManageMembers(userId, projectId);
    }

    @Transactional(readOnly = true)
    public MapProject requireManagePublication(UUID userId, UUID projectId) {
        return requireManageMembers(userId, projectId);
    }

    @Transactional(readOnly = true)
    public MapProject requireViewAudit(UUID userId, UUID projectId) {
        return requireManageMembers(userId, projectId);
    }

    @Transactional(readOnly = true)
    public Optional<ProjectRole> findRole(UUID userId, UUID projectId) {
        return memberRepository.findByProjectIdAndUserId(projectId, userId).map(ProjectMember::getRole);
    }

    @Transactional(readOnly = true)
    public ProjectRole requireRole(UUID userId, UUID projectId) {
        ProjectRole role = resolveRole(userId, findProject(projectId));
        if (role == null) {
            throw new ForbiddenOperationException("Нет доступа к проекту");
        }
        return role;
    }

    private ProjectRole resolveRole(UUID userId, MapProject project) {
        return memberRepository.findByProjectIdAndUserId(project.getId(), userId)
                .map(ProjectMember::getRole)
                .orElse(null);
    }

    private MapProject findProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Проект карты не найден"));
    }
}
