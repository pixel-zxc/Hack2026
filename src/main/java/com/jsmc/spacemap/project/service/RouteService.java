package com.jsmc.spacemap.project.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jsmc.spacemap.project.dto.request.RouteEdgeCreateRequest;
import com.jsmc.spacemap.project.dto.request.RouteEdgeUpdateRequest;
import com.jsmc.spacemap.project.dto.request.RouteNodeCreateRequest;
import com.jsmc.spacemap.project.dto.request.RouteNodeUpdateRequest;
import com.jsmc.spacemap.project.dto.response.RouteEdgeResponse;
import com.jsmc.spacemap.project.dto.response.RouteGraphResponse;
import com.jsmc.spacemap.project.dto.response.RouteNodeResponse;
import com.jsmc.spacemap.project.entity.Floor;
import com.jsmc.spacemap.project.entity.Place;
import com.jsmc.spacemap.project.entity.RouteEdge;
import com.jsmc.spacemap.project.entity.RouteNode;
import com.jsmc.spacemap.project.mapper.RouteEdgeMapper;
import com.jsmc.spacemap.project.mapper.RouteNodeMapper;
import com.jsmc.spacemap.project.repository.RouteEdgeRepository;
import com.jsmc.spacemap.project.repository.RouteNodeRepository;
import com.jsmc.spacemap.project.service.exception.ResourceNotFoundException;

@Service
public class RouteService {
    private final RouteNodeRepository nodeRepository;
    private final RouteEdgeRepository edgeRepository;
    private final FloorService floorService;
    private final PlaceService placeService;
    private final MapProjectService mapProjectService;

    public RouteService(
            RouteNodeRepository nodeRepository,
            RouteEdgeRepository edgeRepository,
            FloorService floorService,
            PlaceService placeService,
            MapProjectService mapProjectService) {
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.floorService = floorService;
        this.placeService = placeService;
        this.mapProjectService = mapProjectService;
    }

    @Transactional(readOnly = true)
    public List<RouteNodeResponse> listNodesByFloor(UUID userId, UUID floorId) {
        floorService.requireFloorAccess(userId, floorId);
        return nodeRepository.findByFloorId(floorId).stream().map(RouteNodeMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RouteNodeResponse getNode(UUID userId, UUID nodeId) {
        return RouteNodeMapper.toResponse(requireNodeAccess(userId, nodeId));
    }

    @Transactional(readOnly = true)
    public List<RouteEdgeResponse> listEdgesByProject(UUID userId, UUID projectId) {
        mapProjectService.requireProjectAccess(userId, projectId);
        return edgeRepository.findByProjectId(projectId).stream().map(RouteEdgeMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<RouteEdgeResponse> listEdgesByFloor(UUID userId, UUID floorId) {
        floorService.requireFloorAccess(userId, floorId);
        return nodeRepository.findByFloorId(floorId).stream()
                .flatMap(n -> edgeRepository.findByFromNodeId(n.getId()).stream())
                .map(RouteEdgeMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RouteGraphResponse getGraph(UUID userId, UUID projectId) {
        mapProjectService.requireProjectAccess(userId, projectId);
        List<RouteNodeResponse> nodes = nodeRepository.findByProjectId(projectId).stream()
                .map(RouteNodeMapper::toResponse).toList();
        List<RouteEdgeResponse> edges = edgeRepository.findByProjectId(projectId).stream()
                .map(RouteEdgeMapper::toResponse).toList();
        return new RouteGraphResponse(projectId, nodes, edges);
    }

    @Transactional
    public RouteNodeResponse createNode(UUID userId, UUID floorId, RouteNodeCreateRequest request) {
        Floor floor = floorService.requireFloorEdit(userId, floorId);
        RouteNode node = new RouteNode();
        node.setFloor(floor);
        node.setX(request.x());
        node.setY(request.y());
        node.setNodeType(normalizeType(request.nodeType(), "WALKWAY"));
        node.setAccessible(request.accessible() == null || request.accessible());
        if (request.placeId() != null) {
            Place place = placeService.requirePlaceEdit(userId, request.placeId());
            if (!place.getFloor().getId().equals(floorId)) {
                throw new IllegalArgumentException("Место должно принадлежать тому же этажу");
            }
            node.setPlace(place);
        }
        return RouteNodeMapper.toResponse(nodeRepository.save(node));
    }

    @Transactional
    public RouteNodeResponse updateNode(UUID userId, UUID nodeId, RouteNodeUpdateRequest request) {
        RouteNode node = requireNodeEdit(userId, nodeId);
        if (request.x() != null) node.setX(request.x());
        if (request.y() != null) node.setY(request.y());
        if (request.nodeType() != null) node.setNodeType(normalizeType(request.nodeType(), node.getNodeType()));
        if (request.accessible() != null) node.setAccessible(request.accessible());
        if (request.placeId() != null) {
            Place place = placeService.requirePlaceEdit(userId, request.placeId());
            if (!place.getFloor().getId().equals(node.getFloor().getId())) {
                throw new IllegalArgumentException("Место должно принадлежать тому же этажу");
            }
            node.setPlace(place);
        }
        return RouteNodeMapper.toResponse(nodeRepository.save(node));
    }

    @Transactional
    public void deleteNode(UUID userId, UUID nodeId) {
        RouteNode node = requireNodeEdit(userId, nodeId);
        edgeRepository.deleteByFromNodeIdOrToNodeId(nodeId, nodeId);
        nodeRepository.delete(node);
    }

    @Transactional
    public RouteEdgeResponse createEdge(UUID userId, UUID projectId, RouteEdgeCreateRequest request) {
        mapProjectService.requireProjectEdit(userId, projectId);
        RouteNode from = requireNodeEdit(userId, request.fromNodeId());
        RouteNode to = requireNodeEdit(userId, request.toNodeId());
        if (from.getId().equals(to.getId())) {
            throw new IllegalArgumentException("Ребро не может соединять узел сам с собой");
        }
        UUID fromProject = from.getFloor().getProject().getId();
        UUID toProject = to.getFloor().getProject().getId();
        if (!fromProject.equals(projectId) || !toProject.equals(projectId)) {
            throw new IllegalArgumentException("Узлы должны принадлежать указанному проекту");
        }
        if (request.cost() != null && request.cost().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Стоимость перехода должна быть положительной");
        }
        String edgeType = normalizeType(request.edgeType(), "WALK");
        if (!from.getFloor().getId().equals(to.getFloor().getId())
                && !"STAIRS".equals(edgeType) && !"ELEVATOR".equals(edgeType) && !"RAMP".equals(edgeType)) {
            throw new IllegalArgumentException("Межэтажное ребро должно быть STAIRS, ELEVATOR или RAMP");
        }
        RouteEdge edge = new RouteEdge();
        edge.setFromNode(from);
        edge.setToNode(to);
        edge.setCost(request.cost() != null ? request.cost() : estimateCost(from, to, edgeType));
        edge.setBidirectional(request.bidirectional() == null || request.bidirectional());
        edge.setEdgeType(edgeType);
        return RouteEdgeMapper.toResponse(edgeRepository.save(edge));
    }

    @Transactional
    public RouteEdgeResponse updateEdge(UUID userId, UUID edgeId, RouteEdgeUpdateRequest request) {
        RouteEdge edge = requireEdgeEdit(userId, edgeId);
        if (request.cost() != null) {
            if (request.cost().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Стоимость перехода должна быть положительной");
            }
            edge.setCost(request.cost());
        }
        if (request.bidirectional() != null) edge.setBidirectional(request.bidirectional());
        if (request.edgeType() != null) edge.setEdgeType(normalizeType(request.edgeType(), edge.getEdgeType()));
        return RouteEdgeMapper.toResponse(edgeRepository.save(edge));
    }

    @Transactional
    public void deleteEdge(UUID userId, UUID edgeId) {
        RouteEdge edge = requireEdgeEdit(userId, edgeId);
        edgeRepository.delete(edge);
    }

    public RouteNode requireNodeAccess(UUID userId, UUID nodeId) {
        RouteNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Узел маршрута не найден"));
        floorService.requireFloorAccess(userId, node.getFloor().getId());
        return node;
    }

    public RouteNode requireNodeEdit(UUID userId, UUID nodeId) {
        RouteNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Узел маршрута не найден"));
        floorService.requireFloorEdit(userId, node.getFloor().getId());
        return node;
    }

    private RouteEdge requireEdgeEdit(UUID userId, UUID edgeId) {
        RouteEdge edge = edgeRepository.findById(edgeId)
                .orElseThrow(() -> new ResourceNotFoundException("Ребро маршрута не найдено"));
        floorService.requireFloorEdit(userId, edge.getFromNode().getFloor().getId());
        return edge;
    }

    private String normalizeType(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim().toUpperCase();
    }

    private BigDecimal estimateCost(RouteNode from, RouteNode to, String edgeType) {
        if (!from.getFloor().getId().equals(to.getFloor().getId())) {
            return switch (edgeType) {
                case "ELEVATOR" -> BigDecimal.valueOf(15);
                case "STAIRS" -> BigDecimal.valueOf(25);
                case "RAMP" -> BigDecimal.valueOf(20);
                default -> BigDecimal.TEN;
            };
        }
        double dx = from.getX().subtract(to.getX()).doubleValue();
        double dy = from.getY().subtract(to.getY()).doubleValue();
        double dist = Math.sqrt(dx * dx + dy * dy);
        return BigDecimal.valueOf(Math.max(dist, 0.01));
    }
}
