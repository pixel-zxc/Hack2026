package com.jsmc.spacemap.project.controller;

import com.jsmc.spacemap.project.dto.request.FloorCreateRequest;
import com.jsmc.spacemap.project.dto.request.FloorOrderRequest;
import com.jsmc.spacemap.project.dto.request.FloorUpdateRequest;
import com.jsmc.spacemap.project.dto.response.AssetResponse;
import com.jsmc.spacemap.project.dto.response.FloorResponse;
import com.jsmc.spacemap.project.service.FloorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class FloorController {
    private final FloorService floorService;

    public FloorController(FloorService floorService) {
        this.floorService = floorService;
    }

    @GetMapping("/projects/{projectId}/floors")
    public List<FloorResponse> listByProject(Authentication authentication, @PathVariable UUID projectId) {
        return floorService.listByProject(userId(authentication), projectId);
    }

    @PostMapping("/projects/{projectId}/floors")
    @ResponseStatus(HttpStatus.CREATED)
    public FloorResponse create(Authentication authentication, @PathVariable UUID projectId,
                                @Valid @RequestBody FloorCreateRequest request) {
        return floorService.create(userId(authentication), projectId, request);
    }

    @PatchMapping("/projects/{projectId}/floors/order")
    public List<FloorResponse> reorder(Authentication authentication, @PathVariable UUID projectId,
                                       @Valid @RequestBody FloorOrderRequest request) {
        return floorService.reorder(userId(authentication), projectId, request);
    }

    @GetMapping("/floors/{floorId}")
    public FloorResponse getById(Authentication authentication, @PathVariable UUID floorId) {
        return floorService.getById(userId(authentication), floorId);
    }

    @PatchMapping("/floors/{floorId}")
    public FloorResponse update(Authentication authentication, @PathVariable UUID floorId,
                                @Valid @RequestBody FloorUpdateRequest request) {
        return floorService.update(userId(authentication), floorId, request);
    }

    @DeleteMapping("/floors/{floorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable UUID floorId) {
        floorService.delete(userId(authentication), floorId);
    }

    @PostMapping(value = "/floors/{floorId}/plan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AssetResponse uploadPlan(Authentication authentication, @PathVariable UUID floorId,
                                    @RequestPart("file") MultipartFile file) {
        return floorService.uploadPlan(userId(authentication), floorId, file);
    }

    @GetMapping("/floors/{floorId}/plan")
    public AssetResponse getPlan(Authentication authentication, @PathVariable UUID floorId) {
        return floorService.getPlan(userId(authentication), floorId);
    }

    @DeleteMapping("/floors/{floorId}/plan")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearPlan(Authentication authentication, @PathVariable UUID floorId) {
        floorService.clearPlan(userId(authentication), floorId);
    }

    private UUID userId(Authentication authentication) {
        return (UUID) authentication.getPrincipal();
    }
}
