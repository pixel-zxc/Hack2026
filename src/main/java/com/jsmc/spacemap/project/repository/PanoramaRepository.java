package com.jsmc.spacemap.project.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jsmc.spacemap.project.entity.Panorama;
import com.jsmc.spacemap.project.entity.PanoramaStatus;

public interface PanoramaRepository extends JpaRepository<Panorama, UUID> {
    List<Panorama> findByFloorIdOrderByNameAsc(UUID floorId);
    List<Panorama> findByPlaceId(UUID placeId);
    List<Panorama> findByFloorIdAndStatus(UUID floorId, PanoramaStatus status);
    boolean existsByFloorId(UUID floorId);
    boolean existsByPlaceId(UUID placeId);
    boolean existsByAssetId(UUID assetId);
}
