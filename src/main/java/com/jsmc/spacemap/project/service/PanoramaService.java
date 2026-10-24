package com.jsmc.spacemap.project.service;

import java.math.BigDecimal;
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
import org.springframework.web.multipart.MultipartFile;

import com.jsmc.spacemap.project.dto.request.PanoramaCreateRequest;
import com.jsmc.spacemap.project.dto.request.PanoramaUpdateRequest;
import com.jsmc.spacemap.project.dto.response.AssetResponse;
import com.jsmc.spacemap.project.dto.response.HotspotResponse;
import com.jsmc.spacemap.project.dto.response.PanoramaDetailResponse;
import com.jsmc.spacemap.project.dto.response.PanoramaResponse;
import com.jsmc.spacemap.project.dto.response.TourConnectivityResponse;
import com.jsmc.spacemap.project.entity.Asset;
import com.jsmc.spacemap.project.entity.Floor;
import com.jsmc.spacemap.project.entity.HotSpot;
import com.jsmc.spacemap.project.entity.Panorama;
import com.jsmc.spacemap.project.entity.PanoramaStatus;
import com.jsmc.spacemap.project.entity.Place;
import com.jsmc.spacemap.project.mapper.AssetMapper;
import com.jsmc.spacemap.project.mapper.HotspotMapper;
import com.jsmc.spacemap.project.mapper.PanoramaMapper;
import com.jsmc.spacemap.project.repository.HotspotRepository;
import com.jsmc.spacemap.project.repository.PanoramaRepository;
import com.jsmc.spacemap.project.service.exception.ConflictException;
import com.jsmc.spacemap.project.service.exception.ResourceNotFoundException;

@Service
public class PanoramaService {
    private final PanoramaRepository panoramaRepository;
    private final HotspotRepository hotspotRepository;
    private final FloorService floorService;
    private final PlaceService placeService;
    private final AssetService assetService;

    public PanoramaService(
            PanoramaRepository panoramaRepository,
            HotspotRepository hotspotRepository,
            FloorService floorService,
            PlaceService placeService,
            AssetService assetService) {
        this.panoramaRepository = panoramaRepository;
        this.hotspotRepository = hotspotRepository;
        this.floorService = floorService;
        this.placeService = placeService;
        this.assetService = assetService;
    }

    @Transactional(readOnly = true)
    public List<PanoramaResponse> listByFloor(UUID userId, UUID floorId) {
        floorService.requireFloorAccess(userId, floorId);
        return panoramaRepository.findByFloorIdOrderByNameAsc(floorId).stream()
                .map(PanoramaMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PanoramaResponse> listByPlace(UUID userId, UUID placeId) {
        placeService.requirePlaceAccess(userId, placeId);
        return panoramaRepository.findByPlaceId(placeId).stream()
                .map(PanoramaMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PanoramaDetailResponse getById(UUID userId, UUID panoramaId) {
        Panorama panorama = requirePanoramaAccess(userId, panoramaId);
        List<HotspotResponse> hotspots = hotspotRepository.findByPanoramaIdOrderByCreatedAtAsc(panoramaId).stream()
                .map(HotspotMapper::toResponse)
                .toList();
        return toDetail(panorama, hotspots);
    }

    @Transactional
    public PanoramaResponse create(UUID userId, UUID floorId, PanoramaCreateRequest request) {
        Floor floor = floorService.requireFloorEdit(userId, floorId);
        Asset asset = assetService.requireOwnedAsset(userId, request.assetId());
        Panorama panorama = new Panorama();
        panorama.setFloor(floor);
        panorama.setAsset(asset);
        panorama.setName(request.name() != null ? request.name().trim() : null);
        BigDecimal yaw = request.initialYaw() != null ? request.initialYaw() : BigDecimal.ZERO;
        BigDecimal pitch = request.initialPitch() != null ? request.initialPitch() : BigDecimal.ZERO;
        validateAngles(yaw, pitch);
        panorama.setInitialYaw(yaw);
        panorama.setInitialPitch(pitch);
        panorama.setX(request.x());
        panorama.setY(request.y());
        panorama.setStatus(PanoramaStatus.READY);
        if (request.placeId() != null) {
            Place place = placeService.requirePlaceEdit(userId, request.placeId());
            if (!place.getFloor().getId().equals(floorId)) {
                throw new IllegalArgumentException("Место должно принадлежать тому же этажу");
            }
            panorama.setPlace(place);
        }
        return PanoramaMapper.toResponse(panoramaRepository.save(panorama));
    }

    @Transactional
    public PanoramaResponse uploadAndCreate(UUID userId, UUID floorId, MultipartFile file, String name,
                                            UUID placeId, BigDecimal x, BigDecimal y) {
        Floor floor = floorService.requireFloorEdit(userId, floorId);
        Asset asset = assetService.upload(userId, file, "panoramas");
        Panorama panorama = new Panorama();
        panorama.setFloor(floor);
        panorama.setAsset(asset);
        panorama.setName(name != null ? name.trim() : null);
        panorama.setInitialYaw(BigDecimal.ZERO);
        panorama.setInitialPitch(BigDecimal.ZERO);
        panorama.setX(x);
        panorama.setY(y);
        panorama.setStatus(PanoramaStatus.READY);
        if (placeId != null) {
            Place place = placeService.requirePlaceEdit(userId, placeId);
            if (!place.getFloor().getId().equals(floorId)) {
                throw new IllegalArgumentException("Место должно принадлежать тому же этажу");
            }
            panorama.setPlace(place);
        }
        return PanoramaMapper.toResponse(panoramaRepository.save(panorama));
    }

    @Transactional
    public PanoramaResponse update(UUID userId, UUID panoramaId, PanoramaUpdateRequest request) {
        Panorama panorama = requirePanoramaEdit(userId, panoramaId);
        if (request.name() != null) {
            panorama.setName(request.name().trim());
        }
        if (request.initialYaw() != null) {
            panorama.setInitialYaw(request.initialYaw());
        }
        if (request.initialPitch() != null) {
            panorama.setInitialPitch(request.initialPitch());
        }
        if (request.x() != null) {
            panorama.setX(request.x());
        }
        if (request.y() != null) {
            panorama.setY(request.y());
        }
        if (request.placeId() != null) {
            Place place = placeService.requirePlaceEdit(userId, request.placeId());
            if (!place.getFloor().getId().equals(panorama.getFloor().getId())) {
                throw new IllegalArgumentException("Место должно принадлежать тому же этажу");
            }
            panorama.setPlace(place);
        }
        return PanoramaMapper.toResponse(panoramaRepository.save(panorama));
    }

    @Transactional
    public void delete(UUID userId, UUID panoramaId) {
        Panorama panorama = requirePanoramaEdit(userId, panoramaId);
        if (!hotspotRepository.findByPanoramaIdOrderByCreatedAtAsc(panoramaId).isEmpty()
                || !hotspotRepository.findByTargetPanoramaId(panoramaId).isEmpty()) {
            throw new ConflictException("Нельзя удалить панораму: есть связанные hotspot");
        }
        panoramaRepository.delete(panorama);
    }

    @Transactional(readOnly = true)
    public TourConnectivityResponse checkConnectivity(UUID userId, UUID floorId) {
        floorService.requireFloorAccess(userId, floorId);
        List<Panorama> panoramas = panoramaRepository.findByFloorIdOrderByNameAsc(floorId);
        if (panoramas.isEmpty()) {
            return new TourConnectivityResponse(floorId, 0, 0, true, List.of(), List.of());
        }
        Map<UUID, Set<UUID>> graph = new HashMap<>();
        for (Panorama p : panoramas) {
            graph.put(p.getId(), new HashSet<>());
        }
        int edges = 0;
        for (Panorama p : panoramas) {
            for (HotSpot hs : hotspotRepository.findByPanoramaIdOrderByCreatedAtAsc(p.getId())) {
                if (hs.getTargetPanorama() != null && graph.containsKey(hs.getTargetPanorama().getId())) {
                    UUID targetId = hs.getTargetPanorama().getId();
                    if (graph.get(p.getId()).add(targetId)) {
                        graph.get(targetId).add(p.getId());
                        edges++;
                    }
                }
            }
        }
        Set<UUID> visited = new HashSet<>();
        List<Integer> componentSizes = new ArrayList<>();
        List<UUID> isolated = new ArrayList<>();
        for (UUID id : graph.keySet()) {
            if (visited.contains(id)) {
                continue;
            }
            ArrayDeque<UUID> queue = new ArrayDeque<>();
            queue.add(id);
            visited.add(id);
            int size = 0;
            while (!queue.isEmpty()) {
                UUID current = queue.poll();
                size++;
                for (UUID next : graph.get(current)) {
                    if (visited.add(next)) {
                        queue.add(next);
                    }
                }
            }
            componentSizes.add(size);
            if (size == 1 && graph.get(id).isEmpty()) {
                isolated.add(id);
            }
        }
        boolean fullyConnected = panoramas.size() <= 1
                || (componentSizes.size() == 1 && componentSizes.get(0) == panoramas.size());
        return new TourConnectivityResponse(floorId, panoramas.size(), edges, fullyConnected, isolated, componentSizes);
    }

    public Panorama requirePanoramaAccess(UUID userId, UUID panoramaId) {
        Panorama panorama = panoramaRepository.findById(panoramaId)
                .orElseThrow(() -> new ResourceNotFoundException("Панорама не найдена"));
        floorService.requireFloorAccess(userId, panorama.getFloor().getId());
        return panorama;
    }

    public Panorama requirePanoramaEdit(UUID userId, UUID panoramaId) {
        Panorama panorama = panoramaRepository.findById(panoramaId)
                .orElseThrow(() -> new ResourceNotFoundException("Панорама не найдена"));
        floorService.requireFloorEdit(userId, panorama.getFloor().getId());
        return panorama;
    }

    private void validateAngles(BigDecimal yaw, BigDecimal pitch) {
        if (yaw != null && (yaw.compareTo(BigDecimal.valueOf(-360)) < 0 || yaw.compareTo(BigDecimal.valueOf(360)) > 0)) {
            throw new IllegalArgumentException("initialYaw должен быть в диапазоне [-360, 360]");
        }
        if (pitch != null && (pitch.compareTo(BigDecimal.valueOf(-90)) < 0 || pitch.compareTo(BigDecimal.valueOf(90)) > 0)) {
            throw new IllegalArgumentException("initialPitch должен быть в диапазоне [-90, 90]");
        }
    }

    private PanoramaDetailResponse toDetail(Panorama panorama, List<HotspotResponse> hotspots) {
        return new PanoramaDetailResponse(
                panorama.getId(),
                panorama.getPlace() == null ? null : panorama.getPlace().getId(),
                panorama.getFloor().getId(),
                panorama.getAsset().getId(),
                assetService.publicUrl(panorama.getAsset().getId()),
                panorama.getName(),
                panorama.getInitialYaw(),
                panorama.getInitialPitch(),
                panorama.getX(),
                panorama.getY(),
                panorama.getStatus() == null ? null : panorama.getStatus().name(),
                hotspots,
                panorama.getCreatedAt(),
                panorama.getUpdatedAt()
        );
    }
}
