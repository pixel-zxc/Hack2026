package com.jsmc.spacemap.project.controller;

import com.jsmc.spacemap.project.dto.request.PlaceCreateRequest;
import com.jsmc.spacemap.project.dto.request.PlaceUpdateRequest;
import com.jsmc.spacemap.project.dto.response.PlaceResponse;
import com.jsmc.spacemap.project.service.PlaceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class PlaceController {
    private final PlaceService placeService;

    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @GetMapping("/floors/{floorId}/places")
    public List<PlaceResponse> listByFloor(Authentication authentication, @PathVariable UUID floorId,
                                           @RequestParam(required = false) String category,
                                           @RequestParam(required = false) String q) {
        return placeService.listByFloor(userId(authentication), floorId, category, q);
    }

    @PostMapping("/floors/{floorId}/places")
    @ResponseStatus(HttpStatus.CREATED)
    public PlaceResponse create(Authentication authentication, @PathVariable UUID floorId,
                                @Valid @RequestBody PlaceCreateRequest request) {
        return placeService.create(userId(authentication), floorId, request);
    }

    @GetMapping("/places/{placeId}")
    public PlaceResponse getById(Authentication authentication, @PathVariable UUID placeId) {
        return placeService.getById(userId(authentication), placeId);
    }

    @PatchMapping("/places/{placeId}")
    public PlaceResponse update(Authentication authentication, @PathVariable UUID placeId,
                                @Valid @RequestBody PlaceUpdateRequest request) {
        return placeService.update(userId(authentication), placeId, request);
    }

    @DeleteMapping("/places/{placeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable UUID placeId) {
        placeService.delete(userId(authentication), placeId);
    }

    private UUID userId(Authentication authentication) {
        return (UUID) authentication.getPrincipal();
    }
}
