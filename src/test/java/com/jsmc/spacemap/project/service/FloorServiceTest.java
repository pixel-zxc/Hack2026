package com.jsmc.spacemap.project.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.jsmc.spacemap.project.dto.request.FloorCreateRequest;
import com.jsmc.spacemap.project.dto.request.FloorOrderRequest;
import com.jsmc.spacemap.project.dto.request.FloorUpdateRequest;
import com.jsmc.spacemap.project.dto.response.FloorResponse;
import com.jsmc.spacemap.project.entity.Asset;
import com.jsmc.spacemap.project.entity.Floor;
import com.jsmc.spacemap.project.entity.MapProject;
import com.jsmc.spacemap.project.entity.Organization;
import com.jsmc.spacemap.project.entity.User;
import com.jsmc.spacemap.project.repository.FloorRepository;
import com.jsmc.spacemap.project.repository.PanoramaRepository;
import com.jsmc.spacemap.project.repository.PlaceRepository;
import com.jsmc.spacemap.project.repository.RouteNodeRepository;
import com.jsmc.spacemap.project.service.exception.ConflictException;
import com.jsmc.spacemap.project.service.exception.ForbiddenOperationException;
import com.jsmc.spacemap.project.service.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class FloorServiceTest {

    @Mock FloorRepository floorRepository;
    @Mock MapProjectService mapProjectService;
    @Mock AssetService assetService;
    @Mock PlaceRepository placeRepository;
    @Mock PanoramaRepository panoramaRepository;
    @Mock RouteNodeRepository routeNodeRepository;

    @InjectMocks FloorService floorService;

    UUID userId;
    UUID projectId;
    UUID floorId;
    MapProject project;
    Floor floor;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        floorId = UUID.randomUUID();
        Organization org = new Organization();
        org.setId(UUID.randomUUID());
        project = new MapProject();
        project.setId(projectId);
        project.setOrganization(org);
        floor = new Floor();
        floor.setId(floorId);
        floor.setProject(project);
        floor.setName("1");
        floor.setSortOrder(0);
    }

    @Test
    void createFloor_success() {
        when(mapProjectService.requireProjectEdit(userId, projectId)).thenReturn(project);
        when(floorRepository.countByProjectId(projectId)).thenReturn(0L);
        when(floorRepository.save(any(Floor.class))).thenAnswer(inv -> {
            Floor f = inv.getArgument(0);
            f.setId(UUID.randomUUID());
            return f;
        });

        FloorCreateRequest request = new FloorCreateRequest("Этаж 1", null, null, BigDecimal.TEN, BigDecimal.TEN);
        FloorResponse response = floorService.create(userId, projectId, request);

        assertEquals("Этаж 1", response.name());
        assertEquals(0, response.sortOrder());
        verify(mapProjectService).requireProjectEdit(userId, projectId);
    }

    @Test
    void updateFloor_success() {
        when(floorRepository.findById(floorId)).thenReturn(Optional.of(floor));
        when(mapProjectService.requireProjectEdit(userId, projectId)).thenReturn(project);
        when(floorRepository.save(any(Floor.class))).thenAnswer(inv -> inv.getArgument(0));

        FloorUpdateRequest request = new FloorUpdateRequest("Этаж A", 2, null, null, null);
        FloorResponse response = floorService.update(userId, floorId, request);

        assertEquals("Этаж A", response.name());
        assertEquals(2, response.sortOrder());
    }

    @Test
    void reorderFloors_success() {
        Floor f1 = floor;
        Floor f2 = new Floor();
        f2.setId(UUID.randomUUID());
        f2.setProject(project);
        f2.setName("2");
        f2.setSortOrder(1);

        when(mapProjectService.requireProjectEdit(userId, projectId)).thenReturn(project);
        when(floorRepository.findByProjectIdOrderBySortOrderAsc(projectId))
                .thenReturn(List.of(f1, f2))
                .thenReturn(List.of(f2, f1));

        FloorOrderRequest request = new FloorOrderRequest(List.of(f2.getId(), f1.getId()));
        List<FloorResponse> result = floorService.reorder(userId, projectId, request);

        assertEquals(2, result.size());
        ArgumentCaptor<List<Floor>> captor = ArgumentCaptor.forClass(List.class);
        verify(floorRepository).saveAll(captor.capture());
        List<Floor> saved = captor.getValue();
        assertEquals(1, saved.stream().filter(f -> f.getId().equals(f1.getId())).findFirst().orElseThrow().getSortOrder());
        assertEquals(0, saved.stream().filter(f -> f.getId().equals(f2.getId())).findFirst().orElseThrow().getSortOrder());
    }

    @Test
    void accessForeignProject_forbidden() {
        when(mapProjectService.requireProjectEdit(userId, projectId))
                .thenThrow(new ForbiddenOperationException("Нет доступа к организации"));

        FloorCreateRequest request = new FloorCreateRequest("X", null, null, null, null);
        assertThrows(ForbiddenOperationException.class,
                () -> floorService.create(userId, projectId, request));
    }

    @Test
    void viewerCannotEdit() {
        when(mapProjectService.requireProjectEdit(userId, projectId))
                .thenThrow(new ForbiddenOperationException("Роль VIEWER не позволяет изменять данные"));

        assertThrows(ForbiddenOperationException.class,
                () -> floorService.create(userId, projectId, new FloorCreateRequest("X", null, null, null, null)));
    }

    @Test
    void deleteBlockedWhenRelatedDataExists() {
        when(floorRepository.findById(floorId)).thenReturn(Optional.of(floor));
        when(mapProjectService.requireProjectEdit(userId, projectId)).thenReturn(project);
        when(placeRepository.existsByFloorId(floorId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> floorService.delete(userId, floorId));
        verify(floorRepository, never()).delete(any());
    }

    @Test
    void invalidDimensions_rejected() {
        when(mapProjectService.requireProjectEdit(userId, projectId)).thenReturn(project);
        FloorCreateRequest request = new FloorCreateRequest("Bad", null, null, BigDecimal.valueOf(-1), null);
        assertThrows(IllegalArgumentException.class, () -> floorService.create(userId, projectId, request));
    }

    @Test
    void floorNotFound() {
        when(floorRepository.findById(floorId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> floorService.getById(userId, floorId));
    }

    @Test
    void useForeignAsset_forbidden() {
        when(mapProjectService.requireProjectEdit(userId, projectId)).thenReturn(project);
        UUID foreignAssetId = UUID.randomUUID();
        when(assetService.requireOwnedAsset(userId, foreignAssetId))
                .thenThrow(new ForbiddenOperationException("Нет доступа к файлу"));

        FloorCreateRequest request = new FloorCreateRequest("F", null, foreignAssetId, null, null);
        assertThrows(ForbiddenOperationException.class, () -> floorService.create(userId, projectId, request));
    }
}
