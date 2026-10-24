package com.jsmc.spacemap.project.mapper;

import com.jsmc.spacemap.project.dto.response.AssetResponse;
import com.jsmc.spacemap.project.entity.Asset;

public final class AssetMapper {
    private AssetMapper() { }

    public static AssetResponse toResponse(Asset asset, String url) {
        if (asset == null) return null;
        return new AssetResponse(asset.getId(), asset.getOwner().getId(), asset.getContentType(), asset.getSizeBytes(), asset.getStatus() == null ? null : asset.getStatus().name(), url, asset.getCreatedAt());
    }
}
