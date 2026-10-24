package com.jsmc.spacemap.project.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PlaceResponse(
        UUID id,
        UUID floorId,
        String name,
        String category,
        String description,
        JsonNode geometry,
        BigDecimal x,
        BigDecimal y,
        Instant createdAt,
        Instant updatedAt
) {
}
