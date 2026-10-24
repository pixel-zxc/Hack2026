package com.jsmc.spacemap.project.repository;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jsmc.spacemap.project.entity.PublicationVersion;

public interface PublicationVersionRepository extends JpaRepository<PublicationVersion, UUID> {
    List<PublicationVersion> findByProjectIdOrderByVersionDesc(UUID projectId);
    Optional<PublicationVersion> findByProjectIdAndVersion(UUID projectId, Integer version);
    Optional<PublicationVersion> findFirstByProjectIdOrderByVersionDesc(UUID projectId);
    boolean existsByProjectIdAndVersion(UUID projectId, Integer version);
}
