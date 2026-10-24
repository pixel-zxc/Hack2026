package com.jsmc.spacemap.project.controller;

import com.jsmc.spacemap.project.dto.request.HotspotCreateRequest;
import com.jsmc.spacemap.project.dto.request.HotspotUpdateRequest;
import com.jsmc.spacemap.project.dto.response.HotspotResponse;
import com.jsmc.spacemap.project.service.HotspotService;
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
public class HotspotController {
    private final HotspotService hotspotService;

    public HotspotController(HotspotService hotspotService) {
        this.hotspotService = hotspotService;
    }

    @GetMapping("/panoramas/{panoramaId}/hotspots")
    public List<HotspotResponse> list(Authentication authentication, @PathVariable UUID panoramaId) {
        return hotspotService.listByPanorama(userId(authentication), panoramaId);
    }

    @PostMapping("/panoramas/{panoramaId}/hotspots")
    @ResponseStatus(HttpStatus.CREATED)
    public HotspotResponse create(Authentication authentication, @PathVariable UUID panoramaId,
                                  @Valid @RequestBody HotspotCreateRequest request) {
        return hotspotService.create(userId(authentication), panoramaId, request);
    }

    @PatchMapping("/hotspots/{hotspotId}")
    public HotspotResponse update(Authentication authentication, @PathVariable UUID hotspotId,
                                  @Valid @RequestBody HotspotUpdateRequest request) {
        return hotspotService.update(userId(authentication), hotspotId, request);
    }

    @DeleteMapping("/hotspots/{hotspotId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable UUID hotspotId) {
        hotspotService.delete(userId(authentication), hotspotId);
    }

    private UUID userId(Authentication authentication) {
        return (UUID) authentication.getPrincipal();
    }
}
