package com.jsmc.spacemap.project.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String displayName,
        String role,
        Instant createdAt
) {
}
