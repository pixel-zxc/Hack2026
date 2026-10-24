package com.jsmc.spacemap.project.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jsmc.spacemap.project.dto.request.PathFindRequest;
import com.jsmc.spacemap.project.dto.response.PathResponse;
import com.jsmc.spacemap.project.dto.response.PathStepResponse;
import com.jsmc.spacemap.project.entity.RouteEdge;
import com.jsmc.spacemap.project.entity.RouteNode;
import com.jsmc.spacemap.project.repository.RouteEdgeRepository;
import com.jsmc.spacemap.project.repository.RouteNodeRepository;

@Service
public class RouteCalculationService {
    private final RouteNodeRepository nodeRepository;
    private final RouteEdgeRepository edgeRepository;
    private final RouteService routeService;
    private final MapProjectService mapProjectService;

    public RouteCalculationService(
            RouteNodeRepository nodeRepository,
            RouteEdgeRepository edgeRepository,
            RouteService routeService,
            MapProjectService mapProjectService) {
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.routeService = routeService;
        this.mapProjectService = mapProjectService;
    }

    @Transactional(readOnly = true)
    public PathResponse calculate(UUID userId, UUID projectId, PathFindRequest request) {
        mapProjectService.requireProjectAccess(userId, projectId);
        RouteNode from = routeService.requireNodeAccess(userId, request.fromNodeId());
        RouteNode to = routeService.requireNodeAccess(userId, request.toNodeId());
        if (!from.getFloor().getProject().getId().equals(projectId)
                || !to.getFloor().getProject().getId().equals(projectId)) {
            throw new IllegalArgumentException("Узлы должны принадлежать указанному проекту");
        }
        if (from.getId().equals(to.getId())) {
            return new PathResponse(from.getId(), to.getId(), BigDecimal.ZERO, 0, true, List.of(from.getId()), List.of(), List.of());
        }

        boolean accessibleOnly = Boolean.TRUE.equals(request.accessibleOnly());
        boolean preferElevator = Boolean.TRUE.equals(request.preferElevator());
        boolean avoidStairs = accessibleOnly || Boolean.TRUE.equals(request.avoidStairs());

        List<RouteNode> nodes = nodeRepository.findByProjectId(projectId);
        List<RouteEdge> edges = edgeRepository.findByProjectId(projectId);
        Map<UUID, RouteNode> nodeMap = new HashMap<>();
        for (RouteNode n : nodes) {
            nodeMap.put(n.getId(), n);
        }

        Map<UUID, List<Neighbor>> adj = new HashMap<>();
        for (RouteNode n : nodes) {
            adj.put(n.getId(), new ArrayList<>());
        }
        for (RouteEdge e : edges) {
            UUID a = e.getFromNode().getId();
            UUID b = e.getToNode().getId();
            if (!nodeMap.containsKey(a) || !nodeMap.containsKey(b)) {
                continue;
            }
            if (!isEdgeAllowed(e, nodeMap.get(a), nodeMap.get(b), accessibleOnly, avoidStairs)) {
                continue;
            }
            double cost = edgeCost(e, preferElevator);
            adj.get(a).add(new Neighbor(b, cost, e));
            if (Boolean.TRUE.equals(e.getBidirectional())) {
                adj.get(b).add(new Neighbor(a, cost, e));
            }
        }

        Map<UUID, Double> dist = new HashMap<>();
        Map<UUID, UUID> prev = new HashMap<>();
        Map<UUID, RouteEdge> prevEdge = new HashMap<>();
        for (UUID id : nodeMap.keySet()) {
            dist.put(id, Double.POSITIVE_INFINITY);
        }
        dist.put(from.getId(), 0.0);

        PriorityQueue<UUID> queue = new PriorityQueue<>(Comparator.comparingDouble(dist::get));
        queue.add(from.getId());
        Set<UUID> settled = new HashSet<>();

        while (!queue.isEmpty()) {
            UUID u = queue.poll();
            if (!settled.add(u)) {
                continue;
            }
            if (u.equals(to.getId())) {
                break;
            }
            for (Neighbor n : adj.getOrDefault(u, List.of())) {
                double alt = dist.get(u) + n.cost;
                if (alt < dist.get(n.id)) {
                    dist.put(n.id, alt);
                    prev.put(n.id, u);
                    prevEdge.put(n.id, n.edge);
                    queue.add(n.id);
                }
            }
        }

        if (Double.isInfinite(dist.get(to.getId()))) {
            return new PathResponse(from.getId(), to.getId(), null, 0, false, List.of(), List.of(), List.of());
        }

        List<UUID> path = new ArrayList<>();
        for (UUID at = to.getId(); at != null; at = prev.get(at)) {
            path.add(at);
            if (at.equals(from.getId())) {
                break;
            }
        }
        Collections.reverse(path);

        List<PathStepResponse> steps = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < path.size() - 1; i++) {
            UUID a = path.get(i);
            UUID b = path.get(i + 1);
            RouteEdge edge = prevEdge.get(b);
            RouteNode na = nodeMap.get(a);
            RouteNode nb = nodeMap.get(b);
            BigDecimal d = edge.getCost() != null ? edge.getCost() : BigDecimal.ONE;
            total = total.add(d);
            boolean last = i == path.size() - 2;
            steps.add(new PathStepResponse(
                    i + 1,
                    a,
                    b,
                    na.getFloor().getId(),
                    nb.getFloor().getId(),
                    edge.getEdgeType(),
                    nb.getNodeType(),
                    d.setScale(4, RoundingMode.HALF_UP),
                    instruction(na, nb, edge, last)
            ));
        }

        List<UUID> edgeIds = new ArrayList<>();
        for (int i = 0; i < path.size() - 1; i++) {
            RouteEdge pe = prevEdge.get(path.get(i + 1));
            if (pe != null) {
                edgeIds.add(pe.getId());
            }
        }
        return new PathResponse(
                from.getId(),
                to.getId(),
                total.setScale(4, RoundingMode.HALF_UP),
                steps.size(),
                true,
                path,
                edgeIds,
                steps
        );
    }

    private boolean isEdgeAllowed(RouteEdge e, RouteNode from, RouteNode to, boolean accessibleOnly, boolean avoidStairs) {
        String type = e.getEdgeType() == null ? "WALK" : e.getEdgeType().toUpperCase();
        if (accessibleOnly) {
            if (Boolean.FALSE.equals(from.getAccessible()) || Boolean.FALSE.equals(to.getAccessible())) {
                return false;
            }
            if ("STAIRS".equals(type)) {
                return false;
            }
        }
        if (avoidStairs && "STAIRS".equals(type)) {
            return false;
        }
        return true;
    }

    private double edgeCost(RouteEdge e, boolean preferElevator) {
        double cost = e.getCost() != null ? e.getCost().doubleValue() : 1.0;
        if (cost <= 0) {
            cost = 0.01;
        }
        String type = e.getEdgeType() == null ? "WALK" : e.getEdgeType().toUpperCase();
        if (preferElevator) {
            if ("ELEVATOR".equals(type)) {
                cost *= 0.7;
            } else if ("STAIRS".equals(type)) {
                cost *= 1.5;
            }
        }
        return cost;
    }

    private String instruction(RouteNode from, RouteNode to, RouteEdge edge, boolean last) {
        if (last) {
            if (to.getPlace() != null) {
                return "Вы достигли «" + to.getPlace().getName() + "»";
            }
            return "Вы достигли места назначения";
        }
        String type = edge.getEdgeType() == null ? "WALK" : edge.getEdgeType().toUpperCase();
        return switch (type) {
            case "STAIRS" -> "Поднимитесь или спуститесь по лестнице";
            case "ELEVATOR" -> "Воспользуйтесь лифтом";
            case "RAMP" -> "Пройдите по пандусу";
            case "DOOR" -> "Пройдите через дверь";
            default -> {
                if (!from.getFloor().getId().equals(to.getFloor().getId())) {
                    yield "Перейдите на другой этаж";
                }
                if (to.getPlace() != null) {
                    yield "Пройдите к «" + to.getPlace().getName() + "»";
                }
                if ("ENTRANCE".equalsIgnoreCase(to.getNodeType())) {
                    yield "Пройдите к входу/выходу";
                }
                yield "Пройдите к следующей точке";
            }
        };
    }

    private record Neighbor(UUID id, double cost, RouteEdge edge) {
    }
}
