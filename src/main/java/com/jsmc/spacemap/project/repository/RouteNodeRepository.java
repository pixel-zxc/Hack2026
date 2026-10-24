package com.jsmc.spacemap.project.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jsmc.spacemap.project.entity.RouteNode;

public interface RouteNodeRepository extends JpaRepository<RouteNode, UUID> {
    List<RouteNode> findByFloorId(UUID floorId);
    List<RouteNode> findByFloorIdAndAccessibleTrue(UUID floorId);
    List<RouteNode> findByPlaceId(UUID placeId);
    boolean existsByFloorId(UUID floorId);
    boolean existsByPlaceId(UUID placeId);

    @Query("select n from RouteNode n where n.floor.project.id = :projectId")
    List<RouteNode> findByProjectId(@Param("projectId") UUID projectId);
}
