package com.jsmc.spacemap.project.dto.response;

import java.util.UUID;

public record GraphIssueResponse(
        String severity,
        String code,
        String message,
        UUID entityId
) {
}
