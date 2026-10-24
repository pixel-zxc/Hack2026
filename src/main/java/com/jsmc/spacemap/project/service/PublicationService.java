package com.jsmc.spacemap.project.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jsmc.spacemap.project.dto.request.PublishProjectRequest;
import com.jsmc.spacemap.project.dto.response.PublicationVersionResponse;
import com.jsmc.spacemap.project.dto.response.PublishedMapResponse;
import com.jsmc.spacemap.project.entity.Floor;
import com.jsmc.spacemap.project.entity.MapProject;
import com.jsmc.spacemap.project.entity.ProjectStatus;
import com.jsmc.spacemap.project.entity.PublicationVersion;
import com.jsmc.spacemap.project.mapper.PublicationVersionMapper;
import com.jsmc.spacemap.project.repository.FloorRepository;
import com.jsmc.spacemap.project.repository.MapProjectRepository;
import com.jsmc.spacemap.project.repository.PublicationVersionRepository;
import com.jsmc.spacemap.project.service.exception.ConflictException;
import com.jsmc.spacemap.project.service.exception.ResourceNotFoundException;

@Service
public class PublicationService {
    private final MapProjectRepository projectRepository;
    private final PublicationVersionRepository versionRepository;
    private final FloorRepository floorRepository;
    private final ProjectPermissionService permissionService;
    private final ProjectAuditService auditService;
    private final SnapshotBuilder snapshotBuilder;

    public PublicationService(
            MapProjectRepository projectRepository,
            PublicationVersionRepository versionRepository,
            FloorRepository floorRepository,
            ProjectPermissionService permissionService,
            ProjectAuditService auditService,
            SnapshotBuilder snapshotBuilder) {
        this.projectRepository = projectRepository;
        this.versionRepository = versionRepository;
        this.floorRepository = floorRepository;
        this.permissionService = permissionService;
        this.auditService = auditService;
        this.snapshotBuilder = snapshotBuilder;
    }

    @Transactional
    public PublishedMapResponse publish(UUID userId, UUID projectId, PublishProjectRequest request) {
        MapProject project = permissionService.requireManagePublication(userId, projectId);
        List<Floor> floors = floorRepository.findByProjectIdOrderBySortOrderAsc(projectId);
        if (floors.isEmpty()) {
            throw new IllegalArgumentException("Нельзя опубликовать проект без этажей");
        }

        String slug = resolveSlug(project, request == null ? null : request.publicSlug());
        ObjectNode snapshot = snapshotBuilder.build(project);

        int nextVersion = versionRepository.findFirstByProjectIdOrderByVersionDesc(projectId)
                .map(v -> v.getVersion() + 1)
                .orElse(1);

        PublicationVersion version = new PublicationVersion();
        version.setProject(project);
        version.setVersion(nextVersion);
        version.setSnapshot(snapshot);
        version = versionRepository.save(version);

        project.setPublicSlug(slug);
        project.setStatus(ProjectStatus.PUBLISHED);
        projectRepository.save(project);

        auditService.log(userId, project, "PROJECT_PUBLISHED", "PublicationVersion", version.getId(),
                auditService.details("version", nextVersion).put("slug", slug));

        return toPublished(project, version);
    }

    @Transactional
    public void unpublish(UUID userId, UUID projectId) {
        MapProject project = permissionService.requireManagePublication(userId, projectId);
        if (project.getStatus() != ProjectStatus.PUBLISHED) {
            throw new ConflictException("Проект не опубликован");
        }
        project.setStatus(ProjectStatus.DRAFT);
        projectRepository.save(project);
        auditService.log(userId, project, "PROJECT_UNPUBLISHED", "MapProject", projectId);
    }

    @Transactional(readOnly = true)
    public List<PublicationVersionResponse> listVersions(UUID userId, UUID projectId) {
        permissionService.requireView(userId, projectId);
        return versionRepository.findByProjectIdOrderByVersionDesc(projectId).stream()
                .map(PublicationVersionMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PublishedMapResponse getVersion(UUID userId, UUID projectId, int versionNumber) {
        permissionService.requireView(userId, projectId);
        MapProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Проект карты не найден"));
        PublicationVersion version = versionRepository.findByProjectIdAndVersion(projectId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Версия публикации не найдена"));
        return toPublished(project, version);
    }

    @Transactional
    public PublishedMapResponse republishVersion(UUID userId, UUID projectId, int sourceVersion) {
        MapProject project = permissionService.requireManagePublication(userId, projectId);
        PublicationVersion source = versionRepository.findByProjectIdAndVersion(projectId, sourceVersion)
                .orElseThrow(() -> new ResourceNotFoundException("Версия публикации не найдена"));

        int nextVersion = versionRepository.findFirstByProjectIdOrderByVersionDesc(projectId)
                .map(v -> v.getVersion() + 1)
                .orElse(1);

        PublicationVersion version = new PublicationVersion();
        version.setProject(project);
        version.setVersion(nextVersion);
        version.setSnapshot(source.getSnapshot().deepCopy());
        version = versionRepository.save(version);

        if (project.getPublicSlug() == null || project.getPublicSlug().isBlank()) {
            project.setPublicSlug(generateSlug(project.getTitle()));
        }
        project.setStatus(ProjectStatus.PUBLISHED);
        projectRepository.save(project);

        auditService.log(userId, project, "PROJECT_REPUBLISHED", "PublicationVersion", version.getId(),
                auditService.details("fromVersion", sourceVersion).put("toVersion", nextVersion));
        return toPublished(project, version);
    }

    @Transactional(readOnly = true)
    public PublishedMapResponse getPublicLatest(String slug) {
        MapProject project = requirePublishedBySlug(slug);
        PublicationVersion version = versionRepository.findFirstByProjectIdOrderByVersionDesc(project.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Опубликованная версия не найдена"));
        return toPublished(project, version);
    }

    @Transactional(readOnly = true)
    public List<PublicationVersionResponse> listPublicVersions(String slug) {
        MapProject project = requirePublishedBySlug(slug);
        return versionRepository.findByProjectIdOrderByVersionDesc(project.getId()).stream()
                .map(PublicationVersionMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PublishedMapResponse getPublicVersion(String slug, int versionNumber) {
        MapProject project = requirePublishedBySlug(slug);
        PublicationVersion version = versionRepository.findByProjectIdAndVersion(project.getId(), versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Версия публикации не найдена"));
        return toPublished(project, version);
    }

    @Transactional(readOnly = true)
    public JsonNode getPublicSnapshotSection(String slug, Integer versionNumber, String section) {
        PublishedMapResponse map = versionNumber == null
                ? getPublicLatest(slug)
                : getPublicVersion(slug, versionNumber);
        JsonNode snapshot = map.snapshot();
        if (section == null || section.isBlank()) {
            return snapshot;
        }
        JsonNode node = snapshot.get(section);
        if (node == null) {
            throw new ResourceNotFoundException("Секция снимка не найдена: " + section);
        }
        return node;
    }

    public MapProject requirePublishedBySlug(String slug) {
        MapProject project = projectRepository.findByPublicSlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Публичная карта не найдена"));
        if (project.getStatus() != ProjectStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Публичная карта не найдена");
        }
        return project;
    }

    public boolean isAssetInPublishedSnapshot(UUID assetId) {
        List<MapProject> published = projectRepository.findAll().stream()
                .filter(p -> p.getStatus() == ProjectStatus.PUBLISHED)
                .toList();
        for (MapProject p : published) {
            var version = versionRepository.findFirstByProjectIdOrderByVersionDesc(p.getId());
            if (version.isEmpty()) {
                continue;
            }
            JsonNode snapshot = version.get().getSnapshot();
            if (containsAsset(snapshot, assetId)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAsset(JsonNode snapshot, UUID assetId) {
        String id = assetId.toString();
        if (id.equals(snapshot.path("project").path("coverAssetId").asText(null))) {
            return true;
        }
        for (JsonNode f : snapshot.path("floors")) {
            if (id.equals(f.path("planAssetId").asText(null))) {
                return true;
            }
        }
        for (JsonNode p : snapshot.path("panoramas")) {
            if (id.equals(p.path("assetId").asText(null))) {
                return true;
            }
        }
        return false;
    }

    private String resolveSlug(MapProject project, String requested) {
        if (project.getPublicSlug() != null && !project.getPublicSlug().isBlank()) {
            if (requested != null && !requested.isBlank()
                    && !requested.equalsIgnoreCase(project.getPublicSlug())) {
                String candidate = requested.toLowerCase(Locale.ROOT);
                if (projectRepository.existsByPublicSlug(candidate)
                        && !candidate.equals(project.getPublicSlug())) {
                    throw new ConflictException("Slug уже занят");
                }
                return candidate;
            }
            return project.getPublicSlug();
        }
        if (requested != null && !requested.isBlank()) {
            String candidate = requested.toLowerCase(Locale.ROOT);
            if (projectRepository.existsByPublicSlug(candidate)) {
                throw new ConflictException("Slug уже занят");
            }
            return candidate;
        }
        String generated = generateSlug(project.getTitle());
        while (projectRepository.existsByPublicSlug(generated)) {
            generated = generateSlug(project.getTitle());
        }
        return generated;
    }

    private String generateSlug(String title) {
        String base = title == null ? "map" : title.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (base.length() < 3) {
            base = "map";
        }
        if (base.length() > 80) {
            base = base.substring(0, 80);
        }
        int suffix = ThreadLocalRandom.current().nextInt(1000, 9999);
        return base + "-" + suffix;
    }

    private PublishedMapResponse toPublished(MapProject project, PublicationVersion version) {
        return new PublishedMapResponse(
                project.getId(),
                project.getTitle(),
                project.getDescription(),
                project.getPublicSlug(),
                project.getStatus().name(),
                version.getVersion(),
                version.getPublishedAt(),
                version.getSnapshot()
        );
    }
}
