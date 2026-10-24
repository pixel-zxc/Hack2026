package com.jsmc.spacemap.project.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jsmc.spacemap.project.entity.Floor;
import com.jsmc.spacemap.project.entity.HotSpot;
import com.jsmc.spacemap.project.entity.MapProject;
import com.jsmc.spacemap.project.entity.Panorama;
import com.jsmc.spacemap.project.entity.Place;
import com.jsmc.spacemap.project.entity.RouteEdge;
import com.jsmc.spacemap.project.entity.RouteNode;
import com.jsmc.spacemap.project.repository.FloorRepository;
import com.jsmc.spacemap.project.repository.HotspotRepository;
import com.jsmc.spacemap.project.repository.PanoramaRepository;
import com.jsmc.spacemap.project.repository.PlaceRepository;
import com.jsmc.spacemap.project.repository.RouteEdgeRepository;
import com.jsmc.spacemap.project.repository.RouteNodeRepository;

@Component
public class SnapshotBuilder {
    private final FloorRepository floorRepository;
    private final PlaceRepository placeRepository;
    private final PanoramaRepository panoramaRepository;
    private final HotspotRepository hotspotRepository;
    private final RouteNodeRepository routeNodeRepository;
    private final RouteEdgeRepository routeEdgeRepository;
    private final ObjectMapper objectMapper;

    public SnapshotBuilder(
            FloorRepository floorRepository,
            PlaceRepository placeRepository,
            PanoramaRepository panoramaRepository,
            HotspotRepository hotspotRepository,
            RouteNodeRepository routeNodeRepository,
            RouteEdgeRepository routeEdgeRepository,
            ObjectMapper objectMapper) {
        this.floorRepository = floorRepository;
        this.placeRepository = placeRepository;
        this.panoramaRepository = panoramaRepository;
        this.hotspotRepository = hotspotRepository;
        this.routeNodeRepository = routeNodeRepository;
        this.routeEdgeRepository = routeEdgeRepository;
        this.objectMapper = objectMapper;
    }

    public ObjectNode build(MapProject project) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("schemaVersion", 1);
        ObjectNode projectNode = root.putObject("project");
        projectNode.put("id", project.getId().toString());
        projectNode.put("title", project.getTitle());
        if (project.getDescription() != null) {
            projectNode.put("description", project.getDescription());
        }
        if (project.getCoverAsset() != null) {
            projectNode.put("coverAssetId", project.getCoverAsset().getId().toString());
        }

        List<Floor> floors = floorRepository.findByProjectIdOrderBySortOrderAsc(project.getId());
        ArrayNode floorsArr = root.putArray("floors");
        ArrayNode placesArr = root.putArray("places");
        ArrayNode panoramasArr = root.putArray("panoramas");
        ArrayNode hotspotsArr = root.putArray("hotspots");

        for (Floor floor : floors) {
            ObjectNode f = floorsArr.addObject();
            f.put("id", floor.getId().toString());
            f.put("name", floor.getName());
            f.put("sortOrder", floor.getSortOrder());
            if (floor.getPlanAsset() != null) {
                f.put("planAssetId", floor.getPlanAsset().getId().toString());
            }
            if (floor.getWidth() != null) {
                f.put("width", floor.getWidth());
            }
            if (floor.getHeight() != null) {
                f.put("height", floor.getHeight());
            }

            for (Place place : placeRepository.findByFloorIdOrderByNameAsc(floor.getId())) {
                ObjectNode p = placesArr.addObject();
                p.put("id", place.getId().toString());
                p.put("floorId", floor.getId().toString());
                p.put("name", place.getName());
                p.put("category", place.getCategory());
                if (place.getDescription() != null) {
                    p.put("description", place.getDescription());
                }
                if (place.getGeometry() != null) {
                    p.set("geometry", place.getGeometry());
                }
                if (place.getX() != null) {
                    p.put("x", place.getX());
                }
                if (place.getY() != null) {
                    p.put("y", place.getY());
                }
            }

            for (Panorama panorama : panoramaRepository.findByFloorIdOrderByNameAsc(floor.getId())) {
                ObjectNode pan = panoramasArr.addObject();
                pan.put("id", panorama.getId().toString());
                pan.put("floorId", floor.getId().toString());
                if (panorama.getPlace() != null) {
                    pan.put("placeId", panorama.getPlace().getId().toString());
                }
                pan.put("assetId", panorama.getAsset().getId().toString());
                if (panorama.getName() != null) {
                    pan.put("name", panorama.getName());
                }
                if (panorama.getInitialYaw() != null) {
                    pan.put("initialYaw", panorama.getInitialYaw());
                }
                if (panorama.getInitialPitch() != null) {
                    pan.put("initialPitch", panorama.getInitialPitch());
                }
                if (panorama.getX() != null) {
                    pan.put("x", panorama.getX());
                }
                if (panorama.getY() != null) {
                    pan.put("y", panorama.getY());
                }
                if (panorama.getStatus() != null) {
                    pan.put("status", panorama.getStatus().name());
                }

                for (HotSpot hs : hotspotRepository.findByPanoramaIdOrderByCreatedAtAsc(panorama.getId())) {
                    ObjectNode h = hotspotsArr.addObject();
                    h.put("id", hs.getId().toString());
                    h.put("panoramaId", panorama.getId().toString());
                    if (hs.getTargetPanorama() != null) {
                        h.put("targetPanoramaId", hs.getTargetPanorama().getId().toString());
                    }
                    if (hs.getPlace() != null) {
                        h.put("placeId", hs.getPlace().getId().toString());
                    }
                    h.put("yaw", hs.getYaw());
                    h.put("pitch", hs.getPitch());
                    if (hs.getLabel() != null) {
                        h.put("label", hs.getLabel());
                    }
                }
            }
        }

        ArrayNode nodesArr = root.putArray("routeNodes");
        for (RouteNode n : routeNodeRepository.findByProjectId(project.getId())) {
            ObjectNode node = nodesArr.addObject();
            node.put("id", n.getId().toString());
            node.put("floorId", n.getFloor().getId().toString());
            if (n.getPlace() != null) {
                node.put("placeId", n.getPlace().getId().toString());
            }
            node.put("x", n.getX());
            node.put("y", n.getY());
            node.put("nodeType", n.getNodeType());
            node.put("accessible", Boolean.TRUE.equals(n.getAccessible()));
        }

        ArrayNode edgesArr = root.putArray("routeEdges");
        for (RouteEdge e : routeEdgeRepository.findByProjectId(project.getId())) {
            ObjectNode edge = edgesArr.addObject();
            edge.put("id", e.getId().toString());
            edge.put("fromNodeId", e.getFromNode().getId().toString());
            edge.put("toNodeId", e.getToNode().getId().toString());
            edge.put("cost", e.getCost());
            edge.put("bidirectional", Boolean.TRUE.equals(e.getBidirectional()));
            edge.put("edgeType", e.getEdgeType());
        }

        return root;
    }

    public List<UUID> collectAssetIds(ObjectNode snapshot) {
        List<UUID> ids = new ArrayList<>();
        if (snapshot.path("project").has("coverAssetId")) {
            ids.add(UUID.fromString(snapshot.path("project").path("coverAssetId").asText()));
        }
        snapshot.path("floors").forEach(f -> {
            if (f.has("planAssetId")) {
                ids.add(UUID.fromString(f.path("planAssetId").asText()));
            }
        });
        snapshot.path("panoramas").forEach(p -> {
            if (p.has("assetId")) {
                ids.add(UUID.fromString(p.path("assetId").asText()));
            }
        });
        return ids;
    }
}
