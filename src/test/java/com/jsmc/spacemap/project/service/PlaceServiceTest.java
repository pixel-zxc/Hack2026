package com.jsmc.spacemap.project.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jsmc.spacemap.project.dto.request.PlaceCreateRequest;
import com.jsmc.spacemap.project.dto.request.PlaceUpdateRequest;
import com.jsmc.spacemap.project.dto.response.PlaceResponse;
import com.jsmc.spacemap.project.entity.Floor;
import com.jsmc.spacemap.project.entity.MapProject;
import com.jsmc.spacemap.project.entity.Place;
import com.jsmc.spacemap.project.repository.HotspotRepository;
import com.jsmc.spacemap.project.repository.PanoramaRepository;
import com.jsmc.spacemap.project.repository.PlaceRepository;
import com.jsmc.spacemap.project.repository.RouteNodeRepository;
import com.jsmc.spacemap.project.service.exception.ConflictException;
import com.jsmc.spacemap.project.service.exception.ForbiddenOperationException;
import com.jsmc.spacemap.project.service.exception.ResourceNotFoundException;
import com.jsmc.spacemap.project.service.GeometryValidator;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    @Mock PlaceRepository placeRepository;
    @Mock FloorService floorService;
    @Mock PanoramaRepository panoramaRepository;
    @Mock HotspotRepository hotspotRepository;
    @Mock RouteNodeRepository routeNodeRepository;
    @Mock GeometryValidator geometryValidator;

    @InjectMocks PlaceService placeService;

    UUID userId;
    UUID floorId;
    UUID placeId;
    Floor floor;
    Place place;
    ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        floorId = UUID.randomUUID();
        placeId = UUID.randomUUID();
        MapProject project = new MapProject();
        project.setId(UUID.randomUUID());
        floor = new Floor();
        floor.setId(floorId);
        floor.setProject(project);
        place = new Place();
        place.setId(placeId);
        place.setFloor(floor);
        place.setName("Room");
        place.setCategory("ROOM");
    }

    @Test
    void createPlace_success() {
        when(floorService.requireFloorEdit(userId, floorId)).thenReturn(floor);
        when(placeRepository.save(any(Place.class))).thenAnswer(inv -> {
            Place p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        ObjectNode geometry = mapper.createObjectNode();
        geometry.put("type", "Point");
        geometry.putArray("coordinates").add(1.0).add(2.0);

        PlaceCreateRequest request = new PlaceCreateRequest(
                "Кабинет", "room", "desc", geometry, BigDecimal.ONE, BigDecimal.valueOf(2));
        PlaceResponse response = placeService.create(userId, floorId, request);

        assertEquals("Кабинет", response.name());
        assertEquals("ROOM", response.category());
        assertEquals(geometry, response.geometry());
    }

    @Test
    void updatePlace_success() {
        when(placeRepository.findById(placeId)).thenReturn(Optional.of(place));
        when(floorService.requireFloorEdit(userId, floorId)).thenReturn(floor);
        when(placeRepository.save(any(Place.class))).thenAnswer(inv -> inv.getArgument(0));

        PlaceUpdateRequest request = new PlaceUpdateRequest("Новое", "CORRIDOR", null, null, null, null);
        PlaceResponse response = placeService.update(userId, placeId, request);

        assertEquals("Новое", response.name());
        assertEquals("CORRIDOR", response.category());
    }

    @Test
    void deletePlace_success() {
        when(placeRepository.findById(placeId)).thenReturn(Optional.of(place));
        when(floorService.requireFloorEdit(userId, floorId)).thenReturn(floor);
        when(panoramaRepository.existsByPlaceId(placeId)).thenReturn(false);
        when(hotspotRepository.existsByPlaceId(placeId)).thenReturn(false);
        when(routeNodeRepository.existsByPlaceId(placeId)).thenReturn(false);

        placeService.delete(userId, placeId);
        verify(placeRepository).delete(place);
    }

    @Test
    void deletePlace_blockedWhenLinked() {
        when(placeRepository.findById(placeId)).thenReturn(Optional.of(place));
        when(floorService.requireFloorEdit(userId, floorId)).thenReturn(floor);
        when(panoramaRepository.existsByPlaceId(placeId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> placeService.delete(userId, placeId));
        verify(placeRepository, never()).delete(any());
    }

    @Test
    void viewerCannotCreatePlace() {
        when(floorService.requireFloorEdit(userId, floorId))
                .thenThrow(new ForbiddenOperationException("Роль VIEWER не позволяет изменять данные"));
        PlaceCreateRequest request = new PlaceCreateRequest("X", "ROOM", null, null, null, null);
        assertThrows(ForbiddenOperationException.class, () -> placeService.create(userId, floorId, request));
    }

    @Test
    void foreignFloor_forbidden() {
        when(floorService.requireFloorEdit(userId, floorId))
                .thenThrow(new ForbiddenOperationException("Нет доступа к организации"));
        assertThrows(ForbiddenOperationException.class,
                () -> placeService.create(userId, floorId, new PlaceCreateRequest("X", "ROOM", null, null, null, null)));
    }

    @Test
    void placeNotFound() {
        when(placeRepository.findById(placeId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> placeService.getById(userId, placeId));
    }

    @Test
    void emptyNameOnUpdate_rejected() {
        when(placeRepository.findById(placeId)).thenReturn(Optional.of(place));
        when(floorService.requireFloorEdit(userId, floorId)).thenReturn(floor);
        PlaceUpdateRequest request = new PlaceUpdateRequest("  ", null, null, null, null, null);
        assertThrows(IllegalArgumentException.class, () -> placeService.update(userId, placeId, request));
    }
}
