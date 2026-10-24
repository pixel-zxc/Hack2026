package com.jsmc.spacemap.project.dto.response;

import java.util.List;
import java.util.UUID;

public record TourValidationResponse(
        UUID projectId,
        boolean valid,
        List<GraphIssueResponse> errors,
        List<GraphIssueResponse> warnings
) {
}
