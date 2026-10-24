package com.jsmc.spacemap.project.controller;

import com.jsmc.spacemap.project.dto.request.PanoramaCreateRequest;
import com.jsmc.spacemap.project.dto.request.PanoramaUpdateRequest;
import com.jsmc.spacemap.project.dto.response.PanoramaDetailResponse;
import com.jsmc.spacemap.project.dto.response.PanoramaResponse;
import com.jsmc.spacemap.project.dto.response.TourConnectivityResponse;
import com.jsmc.spacemap.project.service.PanoramaService;
import com.jsmc.spacemap.project.service.VirtualTourService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class PanoramaController {
    private final PanoramaService panoramaService;
    private final VirtualTourService virtualTourService;

    public PanoramaController(PanoramaService panoramaService, VirtualTourService virtualTourService) {
        this.panoramaService = panoramaService;
        this.virtualTourService = virtualTourService;
    }

    @GetMapping("/floors/{floorId}/panoramas")
    public List<PanoramaResponse> listByFloor(Authentication authentication, @PathVariable UUID floorId) {
        return panoramaService.listByFloor(userId(authentication), floorId);
    }

    @PostMapping("/floors/{floorId}/panoramas")
    @ResponseStatus(HttpStatus.CREATED)
    public PanoramaResponse create(Authentication authentication, @PathVariable UUID floorId,
                                   @Valid @RequestBody PanoramaCreateRequest request) {
        return panoramaService.create(userId(authentication), floorId, request);
    }

    @PostMapping(value = "/floors/{floorId}/panoramas/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PanoramaResponse upload(Authentication authentication, @PathVariable UUID floorId,
                                   @RequestPart("file") MultipartFile file,
                                   @RequestParam(required = false) String name,
                                   @RequestParam(required = false) UUID placeId,
                                   @RequestParam(required = false) BigDecimal x,
                                   @RequestParam(required = false) BigDecimal y) {
        return panoramaService.uploadAndCreate(userId(authentication), floorId, file, name, placeId, x, y);
    }

    @GetMapping("/floors/{floorId}/tour/connectivity")
    public TourConnectivityResponse connectivity(Authentication authentication, @PathVariable UUID floorId) {
        return panoramaService.checkConnectivity(userId(authentication), floorId);
    }

    @GetMapping("/panoramas/{panoramaId}")
    public PanoramaDetailResponse getById(Authentication authentication, @PathVariable UUID panoramaId) {
        return panoramaService.getById(userId(authentication), panoramaId);
    }

    @PatchMapping("/panoramas/{panoramaId}")
    public PanoramaResponse update(Authentication authentication, @PathVariable UUID panoramaId,
                                   @Valid @RequestBody PanoramaUpdateRequest request) {
        return panoramaService.update(userId(authentication), panoramaId, request);
    }

    @DeleteMapping("/panoramas/{panoramaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable UUID panoramaId) {
        panoramaService.delete(userId(authentication), panoramaId);
    }

    @GetMapping("/places/{placeId}/panoramas")
    public List<PanoramaResponse> listByPlace(Authentication authentication, @PathVariable UUID placeId) {
        return panoramaService.listByPlace(userId(authentication), placeId);
    }

    @GetMapping("/projects/{projectId}/virtual-tour")
    public com.jsmc.spacemap.project.dto.response.VirtualTourResponse tour(
            Authentication authentication, @PathVariable UUID projectId) {
        return virtualTourService.getTour(userId(authentication), projectId);
    }

    @PostMapping("/projects/{projectId}/virtual-tour/validate")
    public com.jsmc.spacemap.project.dto.response.TourValidationResponse validateTour(
            Authentication authentication, @PathVariable UUID projectId) {
        return virtualTourService.validate(userId(authentication), projectId);
    }

    private UUID userId(Authentication authentication) {
        return (UUID) authentication.getPrincipal();
    }
}
