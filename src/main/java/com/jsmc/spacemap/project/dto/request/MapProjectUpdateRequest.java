package com.jsmc.spacemap.project.dto.request;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record MapProjectUpdateRequest(
        @Size(max = 255) String title,
        @Size(max = 10000) String description,
        UUID coverAssetId
) {
}
