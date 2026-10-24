package com.jsmc.spacemap.project.dto.request;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaceCreateRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 100) String category,
        @Size(max = 10000) String description,
        JsonNode geometry,
        BigDecimal x,
        BigDecimal y
) {
}
