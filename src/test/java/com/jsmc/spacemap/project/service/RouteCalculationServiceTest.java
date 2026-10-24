package com.jsmc.spacemap.project.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jsmc.spacemap.project.dto.request.PathFindRequest;
import com.jsmc.spacemap.project.dto.response.PathResponse;
import com.jsmc.spacemap.project.entity.Floor;
import com.jsmc.spacemap.project.entity.MapProject;
import com.jsmc.spacemap.project.entity.RouteEdge;
import com.jsmc.spacemap.project.entity.RouteNode;
import com.jsmc.spacemap.project.repository.RouteEdgeRepository;
import com.jsmc.spacemap.project.repository.RouteNodeRepository;

@ExtendWith(MockitoExtension.class)
class RouteCalculationServiceTest {

    @Mock RouteNodeRepository nodeRepository;
    @Mock RouteEdgeRepository edgeRepository;
    @Mock RouteService routeService;
    @Mock MapProjectService mapProjectService;
    @InjectMocks RouteCalculationService calculationService;

    UUID userId;
    UUID projectId;
    MapProject project;
    Floor floor;
    RouteNode a;
    RouteNode b;
    RouteNode c;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        project = new MapProject();
        project.setId(projectId);
        floor = new Floor();
        floor.setId(UUID.randomUUID());
        floor.setProject(project);

        a = node(UUID.randomUUID(), "A", true);
        b = node(UUID.randomUUID(), "B", true);
        c = node(UUID.randomUUID(), "C", true);
    }

    @Test
    void choosesCheaperPath() {
        RouteEdge ab = edge(a, b, "1", "WALK", true);
        RouteEdge ac = edge(a, c, "10", "WALK", true);
        RouteEdge cb = edge(c, b, "10", "WALK", true);

        when(mapProjectService.requireProjectAccess(userId, projectId)).thenReturn(project);
        when(routeService.requireNodeAccess(userId, a.getId())).thenReturn(a);
        when(routeService.requireNodeAccess(userId, b.getId())).thenReturn(b);
        when(nodeRepository.findByProjectId(projectId)).thenReturn(List.of(a, b, c));
        when(edgeRepository.findByProjectId(projectId)).thenReturn(List.of(ab, ac, cb));

        PathResponse path = calculationService.calculate(userId, projectId,
                new PathFindRequest(a.getId(), b.getId(), false, false, false));

        assertTrue(path.found());
        assertEquals(List.of(a.getId(), b.getId()), path.nodeIds());
        assertEquals(0, path.totalCost().compareTo(BigDecimal.ONE));
    }

    @Test
    void respectsUnidirectionalEdge() {
        RouteEdge ab = edge(a, b, "1", "WALK", false);
        when(mapProjectService.requireProjectAccess(userId, projectId)).thenReturn(project);
        when(routeService.requireNodeAccess(userId, a.getId())).thenReturn(a);
        when(routeService.requireNodeAccess(userId, b.getId())).thenReturn(b);
        when(nodeRepository.findByProjectId(projectId)).thenReturn(List.of(a, b));
        when(edgeRepository.findByProjectId(projectId)).thenReturn(List.of(ab));

        PathResponse forward = calculationService.calculate(userId, projectId,
                new PathFindRequest(a.getId(), b.getId(), false, false, false));
        PathResponse backward = calculationService.calculate(userId, projectId,
                new PathFindRequest(b.getId(), a.getId(), false, false, false));

        assertTrue(forward.found());
        assertFalse(backward.found());
    }

    @Test
    void accessibleOnlySkipsStairs() {
        RouteEdge stairs = edge(a, b, "1", "STAIRS", true);
        when(mapProjectService.requireProjectAccess(userId, projectId)).thenReturn(project);
        when(routeService.requireNodeAccess(userId, a.getId())).thenReturn(a);
        when(routeService.requireNodeAccess(userId, b.getId())).thenReturn(b);
        when(nodeRepository.findByProjectId(projectId)).thenReturn(List.of(a, b));
        when(edgeRepository.findByProjectId(projectId)).thenReturn(List.of(stairs));

        PathResponse path = calculationService.calculate(userId, projectId,
                new PathFindRequest(a.getId(), b.getId(), true, false, false));
        assertFalse(path.found());
    }

    @Test
    void noPathReturnsFoundFalse() {
        when(mapProjectService.requireProjectAccess(userId, projectId)).thenReturn(project);
        when(routeService.requireNodeAccess(userId, a.getId())).thenReturn(a);
        when(routeService.requireNodeAccess(userId, b.getId())).thenReturn(b);
        when(nodeRepository.findByProjectId(projectId)).thenReturn(List.of(a, b));
        when(edgeRepository.findByProjectId(projectId)).thenReturn(List.of());

        PathResponse path = calculationService.calculate(userId, projectId,
                new PathFindRequest(a.getId(), b.getId(), false, false, false));
        assertFalse(path.found());
    }

    private RouteNode node(UUID id, String type, boolean accessible) {
        RouteNode n = new RouteNode();
        n.setId(id);
        n.setFloor(floor);
        n.setX(BigDecimal.ONE);
        n.setY(BigDecimal.ONE);
        n.setNodeType(type);
        n.setAccessible(accessible);
        return n;
    }

    private RouteEdge edge(RouteNode from, RouteNode to, String cost, String type, boolean bidirectional) {
        RouteEdge e = new RouteEdge();
        e.setId(UUID.randomUUID());
        e.setFromNode(from);
        e.setToNode(to);
        e.setCost(new BigDecimal(cost));
        e.setEdgeType(type);
        e.setBidirectional(bidirectional);
        return e;
    }
}
