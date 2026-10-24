package com.jsmc.spacemap.project.service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.jsmc.spacemap.project.dto.request.FloorCreateRequest;
import com.jsmc.spacemap.project.dto.request.FloorOrderRequest;
import com.jsmc.spacemap.project.dto.request.FloorUpdateRequest;
import com.jsmc.spacemap.project.dto.response.AssetResponse;
import com.jsmc.spacemap.project.dto.response.FloorResponse;
import com.jsmc.spacemap.project.entity.Asset;
import com.jsmc.spacemap.project.entity.Floor;
import com.jsmc.spacemap.project.entity.MapProject;
import com.jsmc.spacemap.project.mapper.AssetMapper;
import com.jsmc.spacemap.project.mapper.FloorMapper;
import com.jsmc.spacemap.project.repository.FloorRepository;
import com.jsmc.spacemap.project.repository.PanoramaRepository;
import com.jsmc.spacemap.project.repository.PlaceRepository;
import com.jsmc.spacemap.project.repository.RouteNodeRepository;
import com.jsmc.spacemap.project.service.exception.ConflictException;
import com.jsmc.spacemap.project.service.exception.ResourceNotFoundException;

@Service
public class FloorService {
    private final FloorRepository floorRepository;
    private final MapProjectService mapProjectService;
    private final AssetService assetService;
    private final PlaceRepository placeRepository;
    private final PanoramaRepository panoramaRepository;
    private final RouteNodeRepository routeNodeRepository;

    public FloorService(
            FloorRepository floorRepository,
            MapProjectService mapProjectService,
            AssetService assetService,
            PlaceRepository placeRepository,
            PanoramaRepository panoramaRepository,
            RouteNodeRepository routeNodeRepository) {
        this.floorRepository = floorRepository;
        this.mapProjectService = mapProjectService;
        this.assetService = assetService;
        this.placeRepository = placeRepository;
        this.panoramaRepository = panoramaRepository;
        this.routeNodeRepository = routeNodeRepository;
    }

    @Transactional(readOnly = true)
    public List<FloorResponse> listByProject(UUID userId, UUID projectId) {
        mapProjectService.requireProjectAccess(userId, projectId);
        return floorRepository.findByProjectIdOrderBySortOrderAsc(projectId).stream()
                .map(FloorMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FloorResponse getById(UUID userId, UUID floorId) {
        return FloorMapper.toResponse(requireFloorAccess(userId, floorId));
    }

    @Transactional
    public FloorResponse create(UUID userId, UUID projectId, FloorCreateRequest request) {
        MapProject project = mapProjectService.requireProjectEdit(userId, projectId);
        Floor floor = new Floor();
        floor.setProject(project);
        floor.setName(request.name().trim());
        floor.setSortOrder(request.sortOrder() != null ? request.sortOrder() : nextSortOrder(project.getId()));
        if (request.planAssetId() != null) {
            floor.setPlanAsset(assetService.requireOwnedAsset(userId, request.planAssetId()));
        }
        if (request.width() != null) {
            floor.setWidth(request.width());
        }
        if (request.height() != null) {
            floor.setHeight(request.height());
        }
        validateDimensions(floor.getWidth(), floor.getHeight());
        return FloorMapper.toResponse(floorRepository.save(floor));
    }

    @Transactional
    public FloorResponse update(UUID userId, UUID floorId, FloorUpdateRequest request) {
        Floor floor = requireFloorEdit(userId, floorId);
        if (request.name() != null) {
            String name = request.name().trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Название этажа не может быть пустым");
            }
            floor.setName(name);
        }
        if (request.sortOrder() != null) {
            floor.setSortOrder(request.sortOrder());
        }
        if (request.planAssetId() != null) {
            floor.setPlanAsset(assetService.requireOwnedAsset(userId, request.planAssetId()));
        }
        if (request.width() != null) {
            floor.setWidth(request.width());
        }
        if (request.height() != null) {
            floor.setHeight(request.height());
        }
        validateDimensions(floor.getWidth(), floor.getHeight());
        return FloorMapper.toResponse(floorRepository.save(floor));
    }

    @Transactional
    public List<FloorResponse> reorder(UUID userId, UUID projectId, FloorOrderRequest request) {
        mapProjectService.requireProjectEdit(userId, projectId);
        List<Floor> floors = floorRepository.findByProjectIdOrderBySortOrderAsc(projectId);
        Set<UUID> existing = new HashSet<>();
        for (Floor f : floors) {
            existing.add(f.getId());
        }
        if (request.floorIds().size() != existing.size() || !existing.containsAll(request.floorIds())) {
            throw new IllegalArgumentException("Список этажей должен содержать все этажи проекта без лишних");
        }
        for (int i = 0; i < request.floorIds().size(); i++) {
            UUID id = request.floorIds().get(i);
            for (Floor floor : floors) {
                if (floor.getId().equals(id)) {
                    floor.setSortOrder(i);
                    break;
                }
            }
        }
        floorRepository.saveAll(floors);
        return floorRepository.findByProjectIdOrderBySortOrderAsc(projectId).stream()
                .map(FloorMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssetResponse getPlan(UUID userId, UUID floorId) {
        Floor floor = requireFloorAccess(userId, floorId);
        if (floor.getPlanAsset() == null) {
            throw new ResourceNotFoundException("План этажа не привязан");
        }
        Asset asset = floor.getPlanAsset();
        return AssetMapper.toResponse(asset, assetService.publicUrl(asset.getId()));
    }

    @Transactional
    public void clearPlan(UUID userId, UUID floorId) {
        Floor floor = requireFloorEdit(userId, floorId);
        Asset previous = floor.getPlanAsset();
        floor.setPlanAsset(null);
        floorRepository.save(floor);
        if (previous != null && !assetService.isInUse(previous.getId())) {
            try {
                assetService.deleteIfUnused(userId, previous.getId());
            } catch (RuntimeException ignored) {
            }
        }
    }

    @Transactional
    public AssetResponse uploadPlan(UUID userId, UUID floorId, MultipartFile file) {
        Floor floor = requireFloorEdit(userId, floorId);
        Asset previous = floor.getPlanAsset();
        Asset uploaded = assetService.upload(userId, file, "floor-plans");
        floor.setPlanAsset(uploaded);
        floorRepository.save(floor);
        if (previous != null && !assetService.isInUse(previous.getId())) {
            try {
                assetService.deleteIfUnused(userId, previous.getId());
            } catch (RuntimeException ignored) {
            }
        }
        return AssetMapper.toResponse(uploaded, assetService.publicUrl(uploaded.getId()));
    }

    @Transactional
    public void delete(UUID userId, UUID floorId) {
        Floor floor = requireFloorEdit(userId, floorId);
        if (placeRepository.existsByFloorId(floorId)
                || panoramaRepository.existsByFloorId(floorId)
                || routeNodeRepository.existsByFloorId(floorId)) {
            throw new ConflictException(
                    "Нельзя удалить этаж: есть связанные места, панорамы или узлы маршрутов");
        }
        floorRepository.delete(floor);
    }

    public Floor requireFloorAccess(UUID userId, UUID floorId) {
        Floor floor = floorRepository.findById(floorId)
                .orElseThrow(() -> new ResourceNotFoundException("Этаж не найден"));
        mapProjectService.requireProjectAccess(userId, floor.getProject().getId());
        return floor;
    }

    public Floor requireFloorEdit(UUID userId, UUID floorId) {
        Floor floor = floorRepository.findById(floorId)
                .orElseThrow(() -> new ResourceNotFoundException("Этаж не найден"));
        mapProjectService.requireProjectEdit(userId, floor.getProject().getId());
        return floor;
    }

    private int nextSortOrder(UUID projectId) {
        return (int) floorRepository.countByProjectId(projectId);
    }

    private void validateDimensions(BigDecimal width, BigDecimal height) {
        if (width != null && width.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ширина плана должна быть положительной");
        }
        if (height != null && height.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Высота плана должна быть положительной");
        }
    }
}
