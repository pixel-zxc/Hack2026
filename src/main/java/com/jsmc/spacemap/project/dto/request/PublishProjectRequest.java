package com.jsmc.spacemap.project.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PublishProjectRequest(
        @Size(min = 3, max = 100)
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "slug: только a-z, 0-9 и дефис")
        String publicSlug
) {
}
