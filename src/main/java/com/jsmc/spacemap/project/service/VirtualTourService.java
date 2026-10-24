package com.jsmc.spacemap.project.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jsmc.spacemap.project.dto.response.GraphIssueResponse;
import com.jsmc.spacemap.project.dto.response.HotspotResponse;
import com.jsmc.spacemap.project.dto.response.PanoramaDetailResponse;
import com.jsmc.spacemap.project.dto.response.TourValidationResponse;
import com.jsmc.spacemap.project.dto.response.VirtualTourResponse;
import com.jsmc.spacemap.project.entity.Floor;
import com.jsmc.spacemap.project.entity.HotSpot;
import com.jsmc.spacemap.project.entity.Panorama;
import com.jsmc.spacemap.project.mapper.HotspotMapper;
import com.jsmc.spacemap.project.repository.FloorRepository;
import com.jsmc.spacemap.project.repository.HotspotRepository;
import com.jsmc.spacemap.project.repository.PanoramaRepository;

@Service
public class VirtualTourService {
    private final MapProjectService mapProjectService;
    private final FloorRepository floorRepository;
    private final PanoramaRepository panoramaRepository;
    private final HotspotRepository hotspotRepository;
    private final AssetService assetService;

    public VirtualTourService(
            MapProjectService mapProjectService,
            FloorRepository floorRepository,
            PanoramaRepository panoramaRepository,
            HotspotRepository hotspotRepository,
            AssetService assetService) {
        this.mapProjectService = mapProjectService;
        this.floorRepository = floorRepository;
        this.panoramaRepository = panoramaRepository;
        this.hotspotRepository = hotspotRepository;
        this.assetService = assetService;
    }

    @Transactional(readOnly = true)
    public VirtualTourResponse getTour(UUID userId, UUID projectId) {
        mapProjectService.requireProjectAccess(userId, projectId);
        List<Panorama> panoramas = loadProjectPanoramas(projectId);
        UUID startId = panoramas.isEmpty() ? null : panoramas.get(0).getId();
        List<PanoramaDetailResponse> details = panoramas.stream().map(p -> {
            List<HotspotResponse> hs = hotspotRepository.findByPanoramaIdOrderByCreatedAtAsc(p.getId()).stream()
                    .map(HotspotMapper::toResponse).toList();
            return new PanoramaDetailResponse(
                    p.getId(),
                    p.getPlace() == null ? null : p.getPlace().getId(),
                    p.getFloor().getId(),
                    p.getAsset().getId(),
                    assetService.publicUrl(p.getAsset().getId()),
                    p.getName(),
                    p.getInitialYaw(),
                    p.getInitialPitch(),
                    p.getX(),
                    p.getY(),
                    p.getStatus() == null ? null : p.getStatus().name(),
                    hs,
                    p.getCreatedAt(),
                    p.getUpdatedAt()
            );
        }).toList();
        return new VirtualTourResponse(projectId, startId, details);
    }

    @Transactional(readOnly = true)
    public TourValidationResponse validate(UUID userId, UUID projectId) {
        mapProjectService.requireProjectAccess(userId, projectId);
        List<Panorama> panoramas = loadProjectPanoramas(projectId);
        List<GraphIssueResponse> errors = new ArrayList<>();
        List<GraphIssueResponse> warnings = new ArrayList<>();

        if (panoramas.isEmpty()) {
            errors.add(new GraphIssueResponse("ERROR", "NO_START", "Нет панорам для старта тура", null));
            return new TourValidationResponse(projectId, false, errors, warnings);
        }

        Map<UUID, Panorama> map = new HashMap<>();
        for (Panorama p : panoramas) {
            map.put(p.getId(), p);
        }

        Map<UUID, Set<UUID>> graph = new HashMap<>();
        for (UUID id : map.keySet()) {
            graph.put(id, new HashSet<>());
        }

        for (Panorama p : panoramas) {
            for (HotSpot hs : hotspotRepository.findByPanoramaIdOrderByCreatedAtAsc(p.getId())) {
                if (hs.getTargetPanorama() == null) {
                    continue;
                }
                UUID targetId = hs.getTargetPanorama().getId();
                if (!map.containsKey(targetId)) {
                    errors.add(new GraphIssueResponse("ERROR", "DEAD_LINK",
                            "Hotspot ссылается на панораму вне проекта", hs.getId()));
                } else {
                    graph.get(p.getId()).add(targetId);
                }
            }
        }

        UUID start = panoramas.get(0).getId();
        Set<UUID> visited = new HashSet<>();
        ArrayDeque<UUID> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty()) {
            UUID u = queue.poll();
            for (UUID v : graph.getOrDefault(u, Set.of())) {
                if (visited.add(v)) {
                    queue.add(v);
                }
            }
        }
        for (UUID id : map.keySet()) {
            if (!visited.contains(id)) {
                warnings.add(new GraphIssueResponse("WARNING", "UNREACHABLE",
                        "Панорама недостижима из стартовой", id));
            }
        }

        boolean valid = errors.isEmpty() && !panoramas.isEmpty();
        return new TourValidationResponse(projectId, valid, errors, warnings);
    }

    private List<Panorama> loadProjectPanoramas(UUID projectId) {
        List<Floor> floors = floorRepository.findByProjectIdOrderBySortOrderAsc(projectId);
        List<Panorama> result = new ArrayList<>();
        for (Floor floor : floors) {
            result.addAll(panoramaRepository.findByFloorIdOrderByNameAsc(floor.getId()));
        }
        return result;
    }
}
