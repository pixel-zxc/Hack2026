package com.jsmc.spacemap.project.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jsmc.spacemap.project.entity.ProjectMember;
import com.jsmc.spacemap.project.entity.ProjectRole;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {
    List<ProjectMember> findByProjectIdOrderByCreatedAtAsc(UUID projectId);
    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);
    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);
    long countByProjectIdAndRole(UUID projectId, ProjectRole role);
    Optional<ProjectMember> findByIdAndProjectId(UUID id, UUID projectId);
}
