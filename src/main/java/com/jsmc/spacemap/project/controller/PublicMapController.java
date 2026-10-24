package com.jsmc.spacemap.project.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.jsmc.spacemap.project.dto.response.PublicationVersionResponse;
import com.jsmc.spacemap.project.dto.response.PublishedMapResponse;
import com.jsmc.spacemap.project.service.PublicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/maps")
public class PublicMapController {
    private final PublicationService publicationService;

    public PublicMapController(PublicationService publicationService) {
        this.publicationService = publicationService;
    }

    @GetMapping("/{slug}")
    public PublishedMapResponse getMap(@PathVariable String slug) {
        return publicationService.getPublicLatest(slug);
    }

    @GetMapping("/{slug}/versions")
    public List<PublicationVersionResponse> versions(@PathVariable String slug) {
        return publicationService.listPublicVersions(slug);
    }

    @GetMapping("/{slug}/versions/{version}")
    public PublishedMapResponse version(@PathVariable String slug, @PathVariable int version) {
        return publicationService.getPublicVersion(slug, version);
    }

    @GetMapping("/{slug}/floors")
    public JsonNode floors(@PathVariable String slug) {
        return publicationService.getPublicSnapshotSection(slug, null, "floors");
    }

    @GetMapping("/{slug}/places")
    public JsonNode places(@PathVariable String slug) {
        return publicationService.getPublicSnapshotSection(slug, null, "places");
    }

    @GetMapping("/{slug}/panoramas")
    public JsonNode panoramas(@PathVariable String slug) {
        return publicationService.getPublicSnapshotSection(slug, null, "panoramas");
    }

    @GetMapping("/{slug}/hotspots")
    public JsonNode hotspots(@PathVariable String slug) {
        return publicationService.getPublicSnapshotSection(slug, null, "hotspots");
    }

    @GetMapping("/{slug}/route-graph")
    public JsonNode routeGraph(@PathVariable String slug) {
        JsonNode snapshot = publicationService.getPublicLatest(slug).snapshot();
        return snapshot;
    }
}
