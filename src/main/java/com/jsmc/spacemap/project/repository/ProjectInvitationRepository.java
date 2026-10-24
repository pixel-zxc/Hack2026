package com.jsmc.spacemap.project.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jsmc.spacemap.project.entity.InvitationStatus;
import com.jsmc.spacemap.project.entity.ProjectInvitation;

public interface ProjectInvitationRepository extends JpaRepository<ProjectInvitation, UUID> {
    List<ProjectInvitation> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
    Optional<ProjectInvitation> findByIdAndProjectId(UUID id, UUID projectId);
    Optional<ProjectInvitation> findByTokenHashAndStatus(String tokenHash, InvitationStatus status);
    Optional<ProjectInvitation> findByProjectIdAndEmailIgnoreCaseAndStatus(UUID projectId, String email, InvitationStatus status);
}
