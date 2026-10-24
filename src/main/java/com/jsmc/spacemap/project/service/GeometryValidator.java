package com.jsmc.spacemap.project.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class GeometryValidator {

    public void validate(JsonNode geometry) {
        if (geometry == null || geometry.isNull()) {
            return;
        }
        if (!geometry.isObject()) {
            throw new IllegalArgumentException("geometry должна быть JSON-объектом");
        }
        JsonNode typeNode = geometry.get("type");
        if (typeNode == null || !typeNode.isTextual()) {
            throw new IllegalArgumentException("geometry.type обязателен");
        }
        String type = typeNode.asText();
        JsonNode coordinates = geometry.get("coordinates");
        if (coordinates == null || coordinates.isNull()) {
            throw new IllegalArgumentException("geometry.coordinates обязателен");
        }
        switch (type) {
            case "Point" -> validatePoint(coordinates);
            case "Polygon" -> validatePolygon(coordinates);
            default -> throw new IllegalArgumentException("Поддерживаются geometry.type: Point, Polygon");
        }
    }

    private void validatePoint(JsonNode coordinates) {
        if (!coordinates.isArray() || coordinates.size() < 2) {
            throw new IllegalArgumentException("Point.coordinates должен быть [x, y]");
        }
        requireFinite(coordinates.get(0), "x");
        requireFinite(coordinates.get(1), "y");
    }

    private void validatePolygon(JsonNode coordinates) {
        if (!coordinates.isArray() || coordinates.isEmpty()) {
            throw new IllegalArgumentException("Polygon.coordinates должен быть массивом колец");
        }
        JsonNode ring = coordinates.get(0);
        if (!ring.isArray() || ring.size() < 4) {
            throw new IllegalArgumentException("Полигон должен содержать не менее 4 точек (замкнутый контур)");
        }
        for (JsonNode point : ring) {
            if (!point.isArray() || point.size() < 2) {
                throw new IllegalArgumentException("Каждая точка полигона должна быть [x, y]");
            }
            requireFinite(point.get(0), "x");
            requireFinite(point.get(1), "y");
        }
        JsonNode first = ring.get(0);
        JsonNode last = ring.get(ring.size() - 1);
        if (first.get(0).asDouble() != last.get(0).asDouble()
                || first.get(1).asDouble() != last.get(1).asDouble()) {
            throw new IllegalArgumentException("Контур полигона должен быть замкнут");
        }
    }

    private void requireFinite(JsonNode value, String name) {
        if (value == null || !value.isNumber()) {
            throw new IllegalArgumentException("Координата " + name + " должна быть числом");
        }
        double d = value.asDouble();
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            throw new IllegalArgumentException("Координата " + name + " недопустима");
        }
    }
}
