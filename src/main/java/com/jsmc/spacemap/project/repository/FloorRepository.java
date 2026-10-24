package com.jsmc.spacemap.project.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jsmc.spacemap.project.entity.Floor;

public interface FloorRepository extends JpaRepository<Floor, UUID> {
    List<Floor> findByProjectIdOrderBySortOrderAsc(UUID projectId);
    long countByProjectId(UUID projectId);
    boolean existsByPlanAssetId(UUID planAssetId);
}
