package com.jsmc.spacemap.project.controller;

import com.jsmc.spacemap.project.dto.request.MapProjectCreateRequest;
import com.jsmc.spacemap.project.dto.request.MapProjectUpdateRequest;
import com.jsmc.spacemap.project.dto.response.MapProjectResponse;
import com.jsmc.spacemap.project.service.MapProjectService;
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
@RequestMapping("/api/projects")
public class MapProjectController {
    private final MapProjectService mapProjectService;

    public MapProjectController(MapProjectService mapProjectService) {
        this.mapProjectService = mapProjectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MapProjectResponse create(Authentication authentication, @Valid @RequestBody MapProjectCreateRequest request) {
        return mapProjectService.create(userId(authentication), request);
    }

    @GetMapping("/organization/{organizationId}")
    public List<MapProjectResponse> getByOrganization(Authentication authentication, @PathVariable UUID organizationId) {
        return mapProjectService.getByOrganization(userId(authentication), organizationId);
    }

    @GetMapping("/{projectId}")
    public MapProjectResponse getById(Authentication authentication, @PathVariable UUID projectId) {
        return mapProjectService.getById(userId(authentication), projectId);
    }

    @PatchMapping("/{projectId}")
    public MapProjectResponse update(Authentication authentication, @PathVariable UUID projectId,
                                     @Valid @RequestBody MapProjectUpdateRequest request) {
        return mapProjectService.update(userId(authentication), projectId, request);
    }

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable UUID projectId) {
        mapProjectService.delete(userId(authentication), projectId);
    }

    private UUID userId(Authentication authentication) {
        return (UUID) authentication.getPrincipal();
    }
}
