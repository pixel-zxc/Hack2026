package com.jsmc.spacemap.project.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jsmc.spacemap.project.dto.request.PublishProjectRequest;
import com.jsmc.spacemap.project.dto.response.PublishedMapResponse;
import com.jsmc.spacemap.project.entity.Floor;
import com.jsmc.spacemap.project.entity.MapProject;
import com.jsmc.spacemap.project.entity.ProjectStatus;
import com.jsmc.spacemap.project.entity.PublicationVersion;
import com.jsmc.spacemap.project.repository.FloorRepository;
import com.jsmc.spacemap.project.repository.MapProjectRepository;
import com.jsmc.spacemap.project.repository.PublicationVersionRepository;
import com.jsmc.spacemap.project.service.exception.ConflictException;
import com.jsmc.spacemap.project.service.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class PublicationServiceTest {

    @Mock MapProjectRepository projectRepository;
    @Mock PublicationVersionRepository versionRepository;
    @Mock FloorRepository floorRepository;
    @Mock ProjectPermissionService permissionService;
    @Mock ProjectAuditService auditService;
    @Mock SnapshotBuilder snapshotBuilder;
    @InjectMocks PublicationService publicationService;

    UUID userId;
    UUID projectId;
    MapProject project;
    ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        project = new MapProject();
        project.setId(projectId);
        project.setTitle("Demo Map");
        project.setStatus(ProjectStatus.DRAFT);
    }

    @Test
    void publishCreatesVersionAndSetsStatus() {
        when(permissionService.requireManagePublication(userId, projectId)).thenReturn(project);
        Floor floor = new Floor();
        floor.setId(UUID.randomUUID());
        when(floorRepository.findByProjectIdOrderBySortOrderAsc(projectId)).thenReturn(List.of(floor));
        ObjectNode snapshot = mapper.createObjectNode();
        snapshot.put("schemaVersion", 1);
        when(snapshotBuilder.build(project)).thenReturn(snapshot);
        when(versionRepository.findFirstByProjectIdOrderByVersionDesc(projectId)).thenReturn(Optional.empty());
        when(projectRepository.existsByPublicSlug(any())).thenReturn(false);
        when(versionRepository.save(any(PublicationVersion.class))).thenAnswer(inv -> {
            PublicationVersion v = inv.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });
        when(projectRepository.save(any(MapProject.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auditService.details(any(), any())).thenReturn(mapper.createObjectNode());

        PublishedMapResponse response = publicationService.publish(userId, projectId, new PublishProjectRequest(null));

        assertEquals(ProjectStatus.PUBLISHED, project.getStatus());
        assertEquals(1, response.version());
        ArgumentCaptor<PublicationVersion> captor = ArgumentCaptor.forClass(PublicationVersion.class);
        verify(versionRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getVersion());
    }

    @Test
    void publishWithoutFloorsFails() {
        when(permissionService.requireManagePublication(userId, projectId)).thenReturn(project);
        when(floorRepository.findByProjectIdOrderBySortOrderAsc(projectId)).thenReturn(List.of());
        assertThrows(IllegalArgumentException.class,
                () -> publicationService.publish(userId, projectId, new PublishProjectRequest(null)));
    }

    @Test
    void publicAccessRequiresPublishedStatus() {
        project.setPublicSlug("demo");
        project.setStatus(ProjectStatus.DRAFT);
        when(projectRepository.findByPublicSlug("demo")).thenReturn(Optional.of(project));
        assertThrows(ResourceNotFoundException.class, () -> publicationService.getPublicLatest("demo"));
    }

    @Test
    void unpublishOnlyWhenPublished() {
        when(permissionService.requireManagePublication(userId, projectId)).thenReturn(project);
        project.setStatus(ProjectStatus.DRAFT);
        assertThrows(ConflictException.class, () -> publicationService.unpublish(userId, projectId));
    }

    @Test
    void unpublishSuccess() {
        when(permissionService.requireManagePublication(userId, projectId)).thenReturn(project);
        project.setStatus(ProjectStatus.PUBLISHED);
        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        publicationService.unpublish(userId, projectId);
        assertEquals(ProjectStatus.DRAFT, project.getStatus());
    }
}
