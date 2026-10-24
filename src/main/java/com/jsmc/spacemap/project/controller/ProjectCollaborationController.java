package com.jsmc.spacemap.project.controller;

import com.jsmc.spacemap.project.dto.request.AcceptInvitationRequest;
import com.jsmc.spacemap.project.dto.request.ProjectInviteRequest;
import com.jsmc.spacemap.project.dto.request.ProjectMemberRoleUpdateRequest;
import com.jsmc.spacemap.project.dto.request.TransferOwnershipRequest;
import com.jsmc.spacemap.project.dto.response.ProjectAuditLogResponse;
import com.jsmc.spacemap.project.dto.response.ProjectInvitationResponse;
import com.jsmc.spacemap.project.dto.response.ProjectMemberResponse;
import com.jsmc.spacemap.project.service.ProjectAuditService;
import com.jsmc.spacemap.project.service.ProjectCollaborationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ProjectCollaborationController {
    private final ProjectCollaborationService collaborationService;
    private final ProjectAuditService auditService;

    public ProjectCollaborationController(
            ProjectCollaborationService collaborationService,
            ProjectAuditService auditService) {
        this.collaborationService = collaborationService;
        this.auditService = auditService;
    }

    @GetMapping("/projects/{projectId}/members")
    public List<ProjectMemberResponse> listMembers(Authentication authentication, @PathVariable UUID projectId) {
        return collaborationService.listMembers(userId(authentication), projectId);
    }

    @GetMapping("/projects/{projectId}/members/{memberId}")
    public ProjectMemberResponse getMember(Authentication authentication,
                                           @PathVariable UUID projectId,
                                           @PathVariable UUID memberId) {
        return collaborationService.getMember(userId(authentication), projectId, memberId);
    }

    @PatchMapping("/projects/{projectId}/members/{memberId}/role")
    public ProjectMemberResponse updateRole(Authentication authentication,
                                            @PathVariable UUID projectId,
                                            @PathVariable UUID memberId,
                                            @Valid @RequestBody ProjectMemberRoleUpdateRequest request) {
        return collaborationService.updateRole(userId(authentication), projectId, memberId, request);
    }

    @DeleteMapping("/projects/{projectId}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(Authentication authentication,
                             @PathVariable UUID projectId,
                             @PathVariable UUID memberId) {
        collaborationService.removeMember(userId(authentication), projectId, memberId);
    }

    @PostMapping("/projects/{projectId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectInvitationResponse invite(Authentication authentication,
                                            @PathVariable UUID projectId,
                                            @Valid @RequestBody ProjectInviteRequest request) {
        return collaborationService.invite(userId(authentication), projectId, request);
    }

    @GetMapping("/projects/{projectId}/invitations")
    public List<ProjectInvitationResponse> listInvitations(Authentication authentication,
                                                           @PathVariable UUID projectId) {
        return collaborationService.listInvitations(userId(authentication), projectId);
    }

    @PostMapping("/projects/{projectId}/invitations/{invitationId}/revoke")
    public ProjectInvitationResponse revoke(Authentication authentication,
                                            @PathVariable UUID projectId,
                                            @PathVariable UUID invitationId) {
        return collaborationService.revokeInvitation(userId(authentication), projectId, invitationId);
    }

    @PostMapping("/invitations/accept")
    public ProjectMemberResponse accept(Authentication authentication,
                                        @Valid @RequestBody AcceptInvitationRequest request) {
        return collaborationService.acceptInvitation(userId(authentication), request);
    }

    @PostMapping("/projects/{projectId}/transfer-ownership")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void transfer(Authentication authentication,
                         @PathVariable UUID projectId,
                         @Valid @RequestBody TransferOwnershipRequest request) {
        collaborationService.transferOwnership(userId(authentication), projectId, request);
    }

    @GetMapping("/projects/{projectId}/audit-log")
    public Page<ProjectAuditLogResponse> auditLog(Authentication authentication,
                                                  @PathVariable UUID projectId,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return auditService.list(userId(authentication), projectId, page, size);
    }

    private UUID userId(Authentication authentication) {
        return (UUID) authentication.getPrincipal();
    }
}
