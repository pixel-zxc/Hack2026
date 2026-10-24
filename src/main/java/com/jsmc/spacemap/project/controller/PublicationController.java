package com.jsmc.spacemap.project.controller;

import com.jsmc.spacemap.project.dto.request.PublishProjectRequest;
import com.jsmc.spacemap.project.dto.response.PublicationVersionResponse;
import com.jsmc.spacemap.project.dto.response.PublishedMapResponse;
import com.jsmc.spacemap.project.service.PublicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}")
public class PublicationController {
    private final PublicationService publicationService;

    public PublicationController(PublicationService publicationService) {
        this.publicationService = publicationService;
    }

    @PostMapping("/publish")
    @ResponseStatus(HttpStatus.CREATED)
    public PublishedMapResponse publish(Authentication authentication,
                                        @PathVariable UUID projectId,
                                        @Valid @RequestBody(required = false) PublishProjectRequest request) {
        return publicationService.publish(userId(authentication), projectId,
                request == null ? new PublishProjectRequest(null) : request);
    }

    @PostMapping("/unpublish")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unpublish(Authentication authentication, @PathVariable UUID projectId) {
        publicationService.unpublish(userId(authentication), projectId);
    }

    @GetMapping("/publications")
    public List<PublicationVersionResponse> listVersions(Authentication authentication,
                                                         @PathVariable UUID projectId) {
        return publicationService.listVersions(userId(authentication), projectId);
    }

    @GetMapping("/publications/{version}")
    public PublishedMapResponse getVersion(Authentication authentication,
                                           @PathVariable UUID projectId,
                                           @PathVariable int version) {
        return publicationService.getVersion(userId(authentication), projectId, version);
    }

    @PostMapping("/publications/{version}/republish")
    @ResponseStatus(HttpStatus.CREATED)
    public PublishedMapResponse republish(Authentication authentication,
                                          @PathVariable UUID projectId,
                                          @PathVariable int version) {
        return publicationService.republishVersion(userId(authentication), projectId, version);
    }

    private UUID userId(Authentication authentication) {
        return (UUID) authentication.getPrincipal();
    }
}
