package com.jsmc.spacemap.project.service;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jsmc.spacemap.project.dto.request.MapProjectCreateRequest;
import com.jsmc.spacemap.project.dto.request.MapProjectUpdateRequest;
import com.jsmc.spacemap.project.dto.response.MapProjectResponse;
import com.jsmc.spacemap.project.entity.Asset;
import com.jsmc.spacemap.project.entity.MapProject;
import com.jsmc.spacemap.project.entity.Organization;
import com.jsmc.spacemap.project.entity.User;
import com.jsmc.spacemap.project.mapper.MapProjectMapper;
import com.jsmc.spacemap.project.repository.AssetRepository;
import com.jsmc.spacemap.project.repository.MapProjectRepository;
import com.jsmc.spacemap.project.repository.UserRepository;
import com.jsmc.spacemap.project.service.exception.ForbiddenOperationException;
import com.jsmc.spacemap.project.service.exception.ResourceNotFoundException;

@Service
public class MapProjectService {
    private final MapProjectRepository projectRepository;
    private final OrganizationService organizationService;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final ProjectPermissionService permissionService;
    private final ProjectCollaborationService collaborationService;
    private final ProjectAuditService auditService;

    public MapProjectService(
            MapProjectRepository projectRepository,
            OrganizationService organizationService,
            AssetRepository assetRepository,
            UserRepository userRepository,
            ProjectPermissionService permissionService,
            @Lazy ProjectCollaborationService collaborationService,
            ProjectAuditService auditService) {
        this.projectRepository = projectRepository;
        this.organizationService = organizationService;
        this.assetRepository = assetRepository;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
        this.collaborationService = collaborationService;
        this.auditService = auditService;
    }

    @Transactional
    public MapProjectResponse create(UUID userId, MapProjectCreateRequest request) {
        Organization organization = organizationService.requireOrganizationEdit(userId, request.organizationId());
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        MapProject project = new MapProject();
        project.setOrganization(organization);
        project.setTitle(request.title().trim());
        project.setDescription(request.description());
        if (request.coverAssetId() != null) {
            project.setCoverAsset(requireOwnedAsset(userId, request.coverAssetId()));
        }
        project = projectRepository.save(project);
        collaborationService.ensureOwnerMembership(project, creator);
        auditService.log(userId, project, "PROJECT_CREATED", "MapProject", project.getId());
        return MapProjectMapper.toResponse(project);
    }

    @Transactional(readOnly = true)
    public List<MapProjectResponse> getByOrganization(UUID userId, UUID organizationId) {
        organizationService.requireOrganizationAccess(userId, organizationId);
        return projectRepository.findByOrganizationIdOrderByUpdatedAtDesc(organizationId).stream()
                .map(MapProjectMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MapProjectResponse getById(UUID userId, UUID projectId) {
        return MapProjectMapper.toResponse(requireProjectAccess(userId, projectId));
    }

    @Transactional
    public MapProjectResponse update(UUID userId, UUID projectId, MapProjectUpdateRequest request) {
        MapProject project = requireProjectEdit(userId, projectId);
        if (request.title() != null) project.setTitle(request.title().trim());
        if (request.description() != null) project.setDescription(request.description());
        if (request.coverAssetId() != null) project.setCoverAsset(requireOwnedAsset(userId, request.coverAssetId()));
        project = projectRepository.save(project);
        auditService.log(userId, project, "PROJECT_UPDATED", "MapProject", project.getId());
        return MapProjectMapper.toResponse(project);
    }

    @Transactional
    public void delete(UUID userId, UUID projectId) {
        MapProject project = permissionService.requireDeleteProject(userId, projectId);
        auditService.log(userId, project, "PROJECT_DELETED", "MapProject", projectId);
        projectRepository.delete(project);
    }

    public MapProject requireProjectAccess(UUID userId, UUID projectId) {
        return permissionService.requireView(userId, projectId);
    }

    public MapProject requireProjectEdit(UUID userId, UUID projectId) {
        return permissionService.requireEdit(userId, projectId);
    }

    private Asset requireOwnedAsset(UUID userId, UUID assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Файл не найден"));
        if (!asset.getOwner().getId().equals(userId)) {
            throw new ForbiddenOperationException("Нельзя использовать чужой файл");
        }
        return asset;
    }
}
