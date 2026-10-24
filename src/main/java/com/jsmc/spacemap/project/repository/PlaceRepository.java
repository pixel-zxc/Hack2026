package com.jsmc.spacemap.project.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jsmc.spacemap.project.entity.Place;

public interface PlaceRepository extends JpaRepository<Place, UUID> {
    List<Place> findByFloorIdOrderByNameAsc(UUID floorId);
    List<Place> findByFloorIdAndCategoryIgnoreCaseOrderByNameAsc(UUID floorId, String category);
    long countByFloorId(UUID floorId);
    boolean existsByFloorId(UUID floorId);
}
