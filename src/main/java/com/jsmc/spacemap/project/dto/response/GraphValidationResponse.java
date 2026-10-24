package com.jsmc.spacemap.project.dto.response;

import java.util.List;
import java.util.UUID;

public record GraphValidationResponse(
        UUID projectId,
        boolean validForNavigation,
        List<GraphIssueResponse> errors,
        List<GraphIssueResponse> warnings
) {
}
