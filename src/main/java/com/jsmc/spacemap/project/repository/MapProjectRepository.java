package com.jsmc.spacemap.project.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jsmc.spacemap.project.entity.MapProject;
import com.jsmc.spacemap.project.entity.ProjectStatus;

public interface MapProjectRepository extends JpaRepository<MapProject, UUID> {
    List<MapProject> findByOrganizationIdOrderByUpdatedAtDesc(UUID organizationId);
    List<MapProject> findByOrganizationIdAndStatusOrderByUpdatedAtDesc(UUID organizationId, ProjectStatus status);
    Optional<MapProject> findByPublicSlug(String publicSlug);
    boolean existsByPublicSlug(String publicSlug);
    boolean existsByCoverAssetId(UUID coverAssetId);
}
