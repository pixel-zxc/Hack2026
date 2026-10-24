package com.jsmc.spacemap.project.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jsmc.spacemap.project.dto.request.HotspotCreateRequest;
import com.jsmc.spacemap.project.dto.request.HotspotUpdateRequest;
import com.jsmc.spacemap.project.dto.response.HotspotResponse;
import com.jsmc.spacemap.project.entity.HotSpot;
import com.jsmc.spacemap.project.entity.Panorama;
import com.jsmc.spacemap.project.entity.Place;
import com.jsmc.spacemap.project.mapper.HotspotMapper;
import com.jsmc.spacemap.project.repository.HotspotRepository;
import com.jsmc.spacemap.project.service.exception.ResourceNotFoundException;

@Service
public class HotspotService {
    private final HotspotRepository hotspotRepository;
    private final PanoramaService panoramaService;
    private final PlaceService placeService;

    public HotspotService(HotspotRepository hotspotRepository, PanoramaService panoramaService, PlaceService placeService) {
        this.hotspotRepository = hotspotRepository;
        this.panoramaService = panoramaService;
        this.placeService = placeService;
    }

    @Transactional(readOnly = true)
    public List<HotspotResponse> listByPanorama(UUID userId, UUID panoramaId) {
        panoramaService.requirePanoramaAccess(userId, panoramaId);
        return hotspotRepository.findByPanoramaIdOrderByCreatedAtAsc(panoramaId).stream()
                .map(HotspotMapper::toResponse)
                .toList();
    }

    @Transactional
    public HotspotResponse create(UUID userId, UUID panoramaId, HotspotCreateRequest request) {
        Panorama panorama = panoramaService.requirePanoramaEdit(userId, panoramaId);
        HotSpot hotspot = new HotSpot();
        hotspot.setPanorama(panorama);
        hotspot.setYaw(request.yaw());
        hotspot.setPitch(request.pitch());
        hotspot.setLabel(request.label());
        if (request.targetPanoramaId() != null) {
            Panorama target = panoramaService.requirePanoramaEdit(userId, request.targetPanoramaId());
            if (!target.getFloor().getId().equals(panorama.getFloor().getId())) {
                throw new IllegalArgumentException("Целевая панорама должна быть на том же этаже");
            }
            if (target.getId().equals(panoramaId)) {
                throw new IllegalArgumentException("Нельзя ссылаться на ту же панораму");
            }
            hotspot.setTargetPanorama(target);
        }
        if (request.placeId() != null) {
            Place place = placeService.requirePlaceEdit(userId, request.placeId());
            if (!place.getFloor().getId().equals(panorama.getFloor().getId())) {
                throw new IllegalArgumentException("Место должно принадлежать тому же этажу");
            }
            hotspot.setPlace(place);
        }
        return HotspotMapper.toResponse(hotspotRepository.save(hotspot));
    }

    @Transactional
    public HotspotResponse update(UUID userId, UUID hotspotId, HotspotUpdateRequest request) {
        HotSpot hotspot = requireHotspotEdit(userId, hotspotId);
        if (request.yaw() != null) {
            hotspot.setYaw(request.yaw());
        }
        if (request.pitch() != null) {
            hotspot.setPitch(request.pitch());
        }
        if (request.label() != null) {
            hotspot.setLabel(request.label());
        }
        if (request.targetPanoramaId() != null) {
            Panorama target = panoramaService.requirePanoramaEdit(userId, request.targetPanoramaId());
            if (!target.getFloor().getId().equals(hotspot.getPanorama().getFloor().getId())) {
                throw new IllegalArgumentException("Целевая панорама должна быть на том же этаже");
            }
            if (target.getId().equals(hotspot.getPanorama().getId())) {
                throw new IllegalArgumentException("Нельзя ссылаться на ту же панораму");
            }
            hotspot.setTargetPanorama(target);
        }
        if (request.placeId() != null) {
            Place place = placeService.requirePlaceEdit(userId, request.placeId());
            if (!place.getFloor().getId().equals(hotspot.getPanorama().getFloor().getId())) {
                throw new IllegalArgumentException("Место должно принадлежать тому же этажу");
            }
            hotspot.setPlace(place);
        }
        return HotspotMapper.toResponse(hotspotRepository.save(hotspot));
    }

    @Transactional
    public void delete(UUID userId, UUID hotspotId) {
        HotSpot hotspot = requireHotspotEdit(userId, hotspotId);
        hotspotRepository.delete(hotspot);
    }

    private HotSpot requireHotspotEdit(UUID userId, UUID hotspotId) {
        HotSpot hotspot = hotspotRepository.findById(hotspotId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotspot не найден"));
        panoramaService.requirePanoramaEdit(userId, hotspot.getPanorama().getId());
        return hotspot;
    }
}
