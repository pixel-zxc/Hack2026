package com.jsmc.spacemap.project.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jsmc.spacemap.project.entity.HotSpot;

public interface HotspotRepository extends JpaRepository<HotSpot, UUID> {
    List<HotSpot> findByPanoramaIdOrderByCreatedAtAsc(UUID panoramaId);
    List<HotSpot> findByTargetPanoramaId(UUID targetPanoramaId);
    long countByPanoramaId(UUID panoramaId);
    boolean existsByPlaceId(UUID placeId);
}
