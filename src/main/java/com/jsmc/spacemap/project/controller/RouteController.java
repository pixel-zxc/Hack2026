package com.jsmc.spacemap.project.controller;

import com.jsmc.spacemap.project.dto.request.PathFindRequest;
import com.jsmc.spacemap.project.dto.request.RouteEdgeCreateRequest;
import com.jsmc.spacemap.project.dto.request.RouteEdgeUpdateRequest;
import com.jsmc.spacemap.project.dto.request.RouteNodeCreateRequest;
import com.jsmc.spacemap.project.dto.request.RouteNodeUpdateRequest;
import com.jsmc.spacemap.project.dto.response.GraphValidationResponse;
import com.jsmc.spacemap.project.dto.response.PathResponse;
import com.jsmc.spacemap.project.dto.response.RouteEdgeResponse;
import com.jsmc.spacemap.project.dto.response.RouteGraphResponse;
import com.jsmc.spacemap.project.dto.response.RouteNodeResponse;
import com.jsmc.spacemap.project.service.GraphValidationService;
import com.jsmc.spacemap.project.service.RouteCalculationService;
import com.jsmc.spacemap.project.service.RouteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class RouteController {
    private final RouteService routeService;
    private final RouteCalculationService routeCalculationService;
    private final GraphValidationService graphValidationService;

    public RouteController(
            RouteService routeService,
            RouteCalculationService routeCalculationService,
            GraphValidationService graphValidationService) {
        this.routeService = routeService;
        this.routeCalculationService = routeCalculationService;
        this.graphValidationService = graphValidationService;
    }

    @GetMapping("/floors/{floorId}/route-nodes")
    public List<RouteNodeResponse> listNodes(Authentication authentication, @PathVariable UUID floorId) {
        return routeService.listNodesByFloor(userId(authentication), floorId);
    }

    @PostMapping("/floors/{floorId}/route-nodes")
    @ResponseStatus(HttpStatus.CREATED)
    public RouteNodeResponse createNode(Authentication authentication, @PathVariable UUID floorId,
                                        @Valid @RequestBody RouteNodeCreateRequest request) {
        return routeService.createNode(userId(authentication), floorId, request);
    }

    @GetMapping("/route-nodes/{nodeId}")
    public RouteNodeResponse getNode(Authentication authentication, @PathVariable UUID nodeId) {
        return routeService.getNode(userId(authentication), nodeId);
    }

    @PatchMapping("/route-nodes/{nodeId}")
    public RouteNodeResponse updateNode(Authentication authentication, @PathVariable UUID nodeId,
                                        @Valid @RequestBody RouteNodeUpdateRequest request) {
        return routeService.updateNode(userId(authentication), nodeId, request);
    }

    @DeleteMapping("/route-nodes/{nodeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNode(Authentication authentication, @PathVariable UUID nodeId) {
        routeService.deleteNode(userId(authentication), nodeId);
    }

    @GetMapping("/projects/{projectId}/route-edges")
    public List<RouteEdgeResponse> listEdges(Authentication authentication, @PathVariable UUID projectId) {
        return routeService.listEdgesByProject(userId(authentication), projectId);
    }

    @PostMapping("/projects/{projectId}/route-edges")
    @ResponseStatus(HttpStatus.CREATED)
    public RouteEdgeResponse createEdge(Authentication authentication, @PathVariable UUID projectId,
                                        @Valid @RequestBody RouteEdgeCreateRequest request) {
        return routeService.createEdge(userId(authentication), projectId, request);
    }

    @PatchMapping("/route-edges/{edgeId}")
    public RouteEdgeResponse updateEdge(Authentication authentication, @PathVariable UUID edgeId,
                                        @Valid @RequestBody RouteEdgeUpdateRequest request) {
        return routeService.updateEdge(userId(authentication), edgeId, request);
    }

    @DeleteMapping("/route-edges/{edgeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEdge(Authentication authentication, @PathVariable UUID edgeId) {
        routeService.deleteEdge(userId(authentication), edgeId);
    }

    @GetMapping("/projects/{projectId}/route-graph")
    public RouteGraphResponse graph(Authentication authentication, @PathVariable UUID projectId) {
        return routeService.getGraph(userId(authentication), projectId);
    }

    @PostMapping("/projects/{projectId}/routes/calculate")
    public PathResponse calculate(Authentication authentication, @PathVariable UUID projectId,
                                  @Valid @RequestBody PathFindRequest request) {
        return routeCalculationService.calculate(userId(authentication), projectId, request);
    }

    @GetMapping("/projects/{projectId}/route-graph/validate")
    public GraphValidationResponse validate(Authentication authentication, @PathVariable UUID projectId) {
        return graphValidationService.validate(userId(authentication), projectId);
    }

    private UUID userId(Authentication authentication) {
        return (UUID) authentication.getPrincipal();
    }
}
