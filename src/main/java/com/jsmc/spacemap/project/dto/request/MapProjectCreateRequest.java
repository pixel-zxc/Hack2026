package com.jsmc.spacemap.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record MapProjectCreateRequest(
        @NotNull UUID organizationId,
        @NotBlank @Size(max = 255) String title,
        @Size(max = 10000) String description,
        UUID coverAssetId
) {
}
