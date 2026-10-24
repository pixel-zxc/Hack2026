package com.jsmc.spacemap.project.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jsmc.spacemap.project.entity.MapProject;
import com.jsmc.spacemap.project.entity.ProjectMember;
import com.jsmc.spacemap.project.entity.ProjectRole;
import com.jsmc.spacemap.project.repository.MapProjectRepository;
import com.jsmc.spacemap.project.repository.ProjectMemberRepository;
import com.jsmc.spacemap.project.service.exception.ForbiddenOperationException;

@ExtendWith(MockitoExtension.class)
class ProjectPermissionServiceTest {

    @Mock MapProjectRepository projectRepository;
    @Mock ProjectMemberRepository memberRepository;
    @InjectMocks ProjectPermissionService permissionService;

    UUID projectId;
    UUID userId;
    MapProject project;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        userId = UUID.randomUUID();
        project = new MapProject();
        project.setId(projectId);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
    }

    @Test
    void ownerCanEditAndManage() {
        stubRole(ProjectRole.OWNER);
        assertDoesNotThrow(() -> permissionService.requireEdit(userId, projectId));
        assertDoesNotThrow(() -> permissionService.requireManageMembers(userId, projectId));
        assertDoesNotThrow(() -> permissionService.requireDeleteProject(userId, projectId));
    }

    @Test
    void editorCanEditButNotManage() {
        stubRole(ProjectRole.EDITOR);
        assertDoesNotThrow(() -> permissionService.requireEdit(userId, projectId));
        assertThrows(ForbiddenOperationException.class, () -> permissionService.requireManageMembers(userId, projectId));
        assertThrows(ForbiddenOperationException.class, () -> permissionService.requireDeleteProject(userId, projectId));
    }

    @Test
    void viewerOnlyViews() {
        stubRole(ProjectRole.VIEWER);
        assertDoesNotThrow(() -> permissionService.requireView(userId, projectId));
        assertThrows(ForbiddenOperationException.class, () -> permissionService.requireEdit(userId, projectId));
        assertThrows(ForbiddenOperationException.class, () -> permissionService.requireManageMembers(userId, projectId));
    }

    @Test
    void nonMemberForbidden() {
        when(memberRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.empty());
        assertThrows(ForbiddenOperationException.class, () -> permissionService.requireView(userId, projectId));
    }

    private void stubRole(ProjectRole role) {
        ProjectMember m = new ProjectMember();
        m.setRole(role);
        when(memberRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(m));
    }
}
