package com.jsmc.spacemap.project.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jsmc.spacemap.project.dto.request.PlaceCreateRequest;
import com.jsmc.spacemap.project.dto.request.PlaceUpdateRequest;
import com.jsmc.spacemap.project.dto.response.PlaceResponse;
import com.jsmc.spacemap.project.entity.Floor;
import com.jsmc.spacemap.project.entity.Place;
import com.jsmc.spacemap.project.mapper.PlaceMapper;
import com.jsmc.spacemap.project.repository.HotspotRepository;
import com.jsmc.spacemap.project.repository.PanoramaRepository;
import com.jsmc.spacemap.project.repository.PlaceRepository;
import com.jsmc.spacemap.project.repository.RouteNodeRepository;
import com.jsmc.spacemap.project.service.exception.ConflictException;
import com.jsmc.spacemap.project.service.exception.ResourceNotFoundException;

@Service
public class PlaceService {
    private final PlaceRepository placeRepository;
    private final FloorService floorService;
    private final PanoramaRepository panoramaRepository;
    private final HotspotRepository hotspotRepository;
    private final RouteNodeRepository routeNodeRepository;
    private final GeometryValidator geometryValidator;

    public PlaceService(
            PlaceRepository placeRepository,
            FloorService floorService,
            PanoramaRepository panoramaRepository,
            HotspotRepository hotspotRepository,
            RouteNodeRepository routeNodeRepository,
            GeometryValidator geometryValidator) {
        this.placeRepository = placeRepository;
        this.floorService = floorService;
        this.panoramaRepository = panoramaRepository;
        this.hotspotRepository = hotspotRepository;
        this.routeNodeRepository = routeNodeRepository;
        this.geometryValidator = geometryValidator;
    }

    @Transactional(readOnly = true)
    public List<PlaceResponse> listByFloor(UUID userId, UUID floorId, String category, String q) {
        floorService.requireFloorAccess(userId, floorId);
        List<Place> places;
        if (category != null && !category.isBlank()) {
            places = placeRepository.findByFloorIdAndCategoryIgnoreCaseOrderByNameAsc(floorId, category.trim());
        } else {
            places = placeRepository.findByFloorIdOrderByNameAsc(floorId);
        }
        if (q != null && !q.isBlank()) {
            String needle = q.trim().toLowerCase();
            places = places.stream()
                    .filter(p -> p.getName() != null && p.getName().toLowerCase().contains(needle))
                    .toList();
        }
        return places.stream().map(PlaceMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PlaceResponse getById(UUID userId, UUID placeId) {
        return PlaceMapper.toResponse(requirePlaceAccess(userId, placeId));
    }

    @Transactional
    public PlaceResponse create(UUID userId, UUID floorId, PlaceCreateRequest request) {
        Floor floor = floorService.requireFloorEdit(userId, floorId);
        Place place = new Place();
        place.setFloor(floor);
        place.setName(request.name().trim());
        place.setCategory(request.category().trim().toUpperCase());
        place.setDescription(request.description());
        geometryValidator.validate(request.geometry());
        place.setGeometry(request.geometry());
        place.setX(request.x());
        place.setY(request.y());
        return PlaceMapper.toResponse(placeRepository.save(place));
    }

    @Transactional
    public PlaceResponse update(UUID userId, UUID placeId, PlaceUpdateRequest request) {
        Place place = requirePlaceEdit(userId, placeId);
        if (request.name() != null) {
            String name = request.name().trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Название места не может быть пустым");
            }
            place.setName(name);
        }
        if (request.category() != null) {
            String category = request.category().trim();
            if (category.isEmpty()) {
                throw new IllegalArgumentException("Категория не может быть пустой");
            }
            place.setCategory(category.toUpperCase());
        }
        if (request.description() != null) {
            place.setDescription(request.description());
        }
        if (request.geometry() != null) {
            geometryValidator.validate(request.geometry());
            place.setGeometry(request.geometry());
        }
        if (request.x() != null) {
            place.setX(request.x());
        }
        if (request.y() != null) {
            place.setY(request.y());
        }
        return PlaceMapper.toResponse(placeRepository.save(place));
    }

    @Transactional
    public void delete(UUID userId, UUID placeId) {
        Place place = requirePlaceEdit(userId, placeId);
        if (panoramaRepository.existsByPlaceId(placeId)
                || hotspotRepository.existsByPlaceId(placeId)
                || routeNodeRepository.existsByPlaceId(placeId)) {
            throw new ConflictException(
                    "Нельзя удалить место: есть связанные панорамы, hotspot или узлы маршрутов");
        }
        placeRepository.delete(place);
    }

    public Place requirePlaceAccess(UUID userId, UUID placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Место не найдено"));
        floorService.requireFloorAccess(userId, place.getFloor().getId());
        return place;
    }

    public Place requirePlaceEdit(UUID userId, UUID placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Место не найдено"));
        floorService.requireFloorEdit(userId, place.getFloor().getId());
        return place;
    }
}
