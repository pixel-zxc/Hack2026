package com.jsmc.spacemap.project.controller;

import com.jsmc.spacemap.project.dto.response.AssetResponse;
import com.jsmc.spacemap.project.entity.Asset;
import com.jsmc.spacemap.project.mapper.AssetMapper;
import com.jsmc.spacemap.project.service.AssetService;
import com.jsmc.spacemap.project.service.StorageService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@RestController
@RequestMapping("/api/assets")
public class AssetController {
    private final AssetService assetService;
    private final StorageService storageService;

    public AssetController(AssetService assetService, StorageService storageService) {
        this.assetService = assetService;
        this.storageService = storageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AssetResponse upload(Authentication authentication, @RequestPart("file") MultipartFile file) {
        Asset asset = assetService.upload(userId(authentication), file, "uploads");
        return AssetMapper.toResponse(asset, assetService.publicUrl(asset.getId()));
    }

    @GetMapping("/{assetId}")
    public AssetResponse getMeta(Authentication authentication, @PathVariable UUID assetId) {
        return assetService.getResponse(userId(authentication), assetId);
    }

    @GetMapping("/{assetId}/content")
    public ResponseEntity<InputStreamResource> download(Authentication authentication, @PathVariable UUID assetId) {
        Asset asset = assetService.requireOwnedAsset(userId(authentication), assetId);
        InputStream stream = storageService.getObject(asset.getStorageKey());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + assetId + "\"")
                .contentType(MediaType.parseMediaType(asset.getContentType()))
                .contentLength(asset.getSizeBytes())
                .body(new InputStreamResource(stream));
    }

    @DeleteMapping("/{assetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable UUID assetId) {
        assetService.deleteIfUnused(userId(authentication), assetId);
    }

    private UUID userId(Authentication authentication) {
        return (UUID) authentication.getPrincipal();
    }
}
