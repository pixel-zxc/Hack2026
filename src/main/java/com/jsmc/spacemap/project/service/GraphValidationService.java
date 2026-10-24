package com.jsmc.spacemap.project.service;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jsmc.spacemap.project.dto.response.GraphIssueResponse;
import com.jsmc.spacemap.project.dto.response.GraphValidationResponse;
import com.jsmc.spacemap.project.entity.RouteEdge;
import com.jsmc.spacemap.project.entity.RouteNode;
import com.jsmc.spacemap.project.repository.RouteEdgeRepository;
import com.jsmc.spacemap.project.repository.RouteNodeRepository;

@Service
public class GraphValidationService {
    private final RouteNodeRepository nodeRepository;
    private final RouteEdgeRepository edgeRepository;
    private final MapProjectService mapProjectService;

    public GraphValidationService(
            RouteNodeRepository nodeRepository,
            RouteEdgeRepository edgeRepository,
            MapProjectService mapProjectService) {
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.mapProjectService = mapProjectService;
    }

    @Transactional(readOnly = true)
    public GraphValidationResponse validate(UUID userId, UUID projectId) {
        mapProjectService.requireProjectAccess(userId, projectId);
        List<RouteNode> nodes = nodeRepository.findByProjectId(projectId);
        List<RouteEdge> edges = edgeRepository.findByProjectId(projectId);
        List<GraphIssueResponse> errors = new ArrayList<>();
        List<GraphIssueResponse> warnings = new ArrayList<>();

        Map<UUID, RouteNode> nodeMap = new HashMap<>();
        for (RouteNode n : nodes) {
            nodeMap.put(n.getId(), n);
            if (n.getX() == null || n.getY() == null) {
                errors.add(issue("ERROR", "NODE_COORDS", "У узла отсутствуют координаты", n.getId()));
            }
        }

        Map<UUID, Set<UUID>> undirected = new HashMap<>();
        for (RouteNode n : nodes) {
            undirected.put(n.getId(), new HashSet<>());
        }

        for (RouteEdge e : edges) {
            RouteNode from = e.getFromNode();
            RouteNode to = e.getToNode();
            if (from == null || to == null || !nodeMap.containsKey(from.getId()) || !nodeMap.containsKey(to.getId())) {
                errors.add(issue("ERROR", "EDGE_ORPHAN", "Ребро ссылается на отсутствующий узел", e.getId()));
                continue;
            }
            if (!from.getFloor().getProject().getId().equals(projectId)
                    || !to.getFloor().getProject().getId().equals(projectId)) {
                errors.add(issue("ERROR", "EDGE_CROSS_PROJECT", "Ребро связывает узлы разных проектов", e.getId()));
            }
            if (e.getCost() == null || e.getCost().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add(issue("ERROR", "EDGE_COST", "Некорректный вес ребра", e.getId()));
            }
            String type = e.getEdgeType() == null ? "" : e.getEdgeType().toUpperCase();
            if (!from.getFloor().getId().equals(to.getFloor().getId())
                    && !"STAIRS".equals(type) && !"ELEVATOR".equals(type) && !"RAMP".equals(type)) {
                errors.add(issue("ERROR", "EDGE_INTERFLOOR", "Межэтажное ребро с некорректным типом", e.getId()));
            }
            undirected.get(from.getId()).add(to.getId());
            undirected.get(to.getId()).add(from.getId());
        }

        Set<UUID> floorsWithNodes = new HashSet<>();
        for (RouteNode n : nodes) {
            floorsWithNodes.add(n.getFloor().getId());
        }
        if (floorsWithNodes.size() > 1) {
            Set<UUID> floorsLinked = new HashSet<>();
            for (RouteEdge e : edges) {
                if (e.getFromNode() == null || e.getToNode() == null) continue;
                if (!e.getFromNode().getFloor().getId().equals(e.getToNode().getFloor().getId())) {
                    floorsLinked.add(e.getFromNode().getFloor().getId());
                    floorsLinked.add(e.getToNode().getFloor().getId());
                }
            }
            for (UUID floorId : floorsWithNodes) {
                if (!floorsLinked.contains(floorId) && floorsWithNodes.size() > 1) {
                    warnings.add(issue("WARNING", "FLOOR_ISOLATED",
                            "Этаж не связан межэтажными переходами", floorId));
                }
            }
        }

        if (!nodes.isEmpty()) {
            Set<UUID> visited = new HashSet<>();
            ArrayDeque<UUID> queue = new ArrayDeque<>();
            UUID start = nodes.get(0).getId();
            queue.add(start);
            visited.add(start);
            while (!queue.isEmpty()) {
                UUID u = queue.poll();
                for (UUID v : undirected.getOrDefault(u, Set.of())) {
                    if (visited.add(v)) {
                        queue.add(v);
                    }
                }
            }
            int isolated = 0;
            for (RouteNode n : nodes) {
                if (!visited.contains(n.getId())) {
                    isolated++;
                    warnings.add(issue("WARNING", "NODE_UNREACHABLE",
                            "Узел недостижим в ненаправленном графе", n.getId()));
                }
            }
            if (isolated == 0 && nodes.size() > 1 && edges.isEmpty()) {
                warnings.add(issue("WARNING", "NO_EDGES", "В проекте нет рёбер", null));
            }
        }

        boolean valid = errors.isEmpty() && !nodes.isEmpty() && !edges.isEmpty();
        return new GraphValidationResponse(projectId, valid, errors, warnings);
    }

    private GraphIssueResponse issue(String severity, String code, String message, UUID entityId) {
        return new GraphIssueResponse(severity, code, message, entityId);
    }
}
