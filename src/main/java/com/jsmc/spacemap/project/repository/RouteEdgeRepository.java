package com.jsmc.spacemap.project.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jsmc.spacemap.project.entity.RouteEdge;

public interface RouteEdgeRepository extends JpaRepository<RouteEdge, UUID> {
    List<RouteEdge> findByFromNodeId(UUID fromNodeId);
    List<RouteEdge> findByToNodeId(UUID toNodeId);
    List<RouteEdge> findByFromNodeIdOrToNodeId(UUID fromNodeId, UUID toNodeId);
    void deleteByFromNodeIdOrToNodeId(UUID fromNodeId, UUID toNodeId);

    @Query("select e from RouteEdge e where e.fromNode.floor.project.id = :projectId or e.toNode.floor.project.id = :projectId")
    List<RouteEdge> findByProjectId(@Param("projectId") UUID projectId);
}
