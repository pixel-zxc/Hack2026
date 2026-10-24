package com.jsmc.spacemap.project.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jsmc.spacemap.project.entity.ProjectAuditLog;

public interface ProjectAuditLogRepository extends JpaRepository<ProjectAuditLog, UUID> {
    Page<ProjectAuditLog> findByProjectIdOrderByCreatedAtDesc(UUID projectId, Pageable pageable);
}
