package com.jsmc.spacemap.project.controller;

import com.jsmc.spacemap.project.dto.request.OrganizationCreateRequest;
import com.jsmc.spacemap.project.dto.request.OrganizationUpdateRequest;
import com.jsmc.spacemap.project.dto.response.OrganizationResponse;
import com.jsmc.spacemap.project.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
@RequestMapping("/api/organizations")
public class OrganizationController {
    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponse create(Authentication authentication, @Valid @RequestBody OrganizationCreateRequest request) {
        return organizationService.create(userId(authentication), request);
    }

    @GetMapping
    public List<OrganizationResponse> getMine(Authentication authentication) {
        return organizationService.getMine(userId(authentication));
    }

    @GetMapping("/{organizationId}")
    public OrganizationResponse getById(Authentication authentication, @PathVariable UUID organizationId) {
        return organizationService.getById(userId(authentication), organizationId);
    }

    @PatchMapping("/{organizationId}")
    public OrganizationResponse update(Authentication authentication, @PathVariable UUID organizationId,
                                       @Valid @RequestBody OrganizationUpdateRequest request) {
        return organizationService.update(userId(authentication), organizationId, request);
    }

    private UUID userId(Authentication authentication) {
        return (UUID) authentication.getPrincipal();
    }
}
