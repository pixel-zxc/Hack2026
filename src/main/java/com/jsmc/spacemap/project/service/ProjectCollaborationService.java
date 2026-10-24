package com.jsmc.spacemap.project.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jsmc.spacemap.project.dto.request.AcceptInvitationRequest;
import com.jsmc.spacemap.project.dto.request.ProjectInviteRequest;
import com.jsmc.spacemap.project.dto.request.ProjectMemberRoleUpdateRequest;
import com.jsmc.spacemap.project.dto.request.TransferOwnershipRequest;
import com.jsmc.spacemap.project.dto.response.ProjectInvitationResponse;
import com.jsmc.spacemap.project.dto.response.ProjectMemberResponse;
import com.jsmc.spacemap.project.entity.InvitationStatus;
import com.jsmc.spacemap.project.entity.MapProject;
import com.jsmc.spacemap.project.entity.ProjectInvitation;
import com.jsmc.spacemap.project.entity.ProjectMember;
import com.jsmc.spacemap.project.entity.ProjectRole;
import com.jsmc.spacemap.project.entity.User;
import com.jsmc.spacemap.project.mapper.ProjectInvitationMapper;
import com.jsmc.spacemap.project.mapper.ProjectMemberMapper;
import com.jsmc.spacemap.project.repository.ProjectInvitationRepository;
import com.jsmc.spacemap.project.repository.ProjectMemberRepository;
import com.jsmc.spacemap.project.repository.UserRepository;
import com.jsmc.spacemap.project.service.exception.ConflictException;
import com.jsmc.spacemap.project.service.exception.ForbiddenOperationException;
import com.jsmc.spacemap.project.service.exception.ResourceNotFoundException;

@Service
public class ProjectCollaborationService {
    private final ProjectMemberRepository memberRepository;
    private final ProjectInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final ProjectPermissionService permissionService;
    private final ProjectAuditService auditService;
    private final InvitationMailService mailService;

    public ProjectCollaborationService(
            ProjectMemberRepository memberRepository,
            ProjectInvitationRepository invitationRepository,
            UserRepository userRepository,
            ProjectPermissionService permissionService,
            ProjectAuditService auditService,
            InvitationMailService mailService) {
        this.memberRepository = memberRepository;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
        this.auditService = auditService;
        this.mailService = mailService;
    }

    @Transactional
    public void ensureOwnerMembership(MapProject project, User owner) {
        if (memberRepository.existsByProjectIdAndUserId(project.getId(), owner.getId())) {
            return;
        }
        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(owner);
        member.setRole(ProjectRole.OWNER);
        memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> listMembers(UUID userId, UUID projectId) {
        permissionService.requireView(userId, projectId);
        return memberRepository.findByProjectIdOrderByCreatedAtAsc(projectId).stream()
                .map(ProjectMemberMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectMemberResponse getMember(UUID userId, UUID projectId, UUID memberId) {
        permissionService.requireView(userId, projectId);
        ProjectMember member = memberRepository.findByIdAndProjectId(memberId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Участник не найден"));
        return ProjectMemberMapper.toResponse(member);
    }

    @Transactional
    public ProjectMemberResponse updateRole(UUID userId, UUID projectId, UUID memberId, ProjectMemberRoleUpdateRequest request) {
        MapProject project = permissionService.requireManageMembers(userId, projectId);
        if (request.role() == ProjectRole.OWNER) {
            throw new IllegalArgumentException("Назначение владельца выполняется через передачу владения");
        }
        ProjectMember member = memberRepository.findByIdAndProjectId(memberId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Участник не найден"));
        if (member.getRole() == ProjectRole.OWNER) {
            throw new ConflictException("Нельзя изменить роль владельца напрямую");
        }
        ProjectRole previous = member.getRole();
        member.setRole(request.role());
        memberRepository.save(member);
        auditService.log(userId, project, "MEMBER_ROLE_CHANGED", "ProjectMember", member.getId(),
                auditService.details("from", previous.name()).put("to", request.role().name()));
        return ProjectMemberMapper.toResponse(member);
    }

    @Transactional
    public void removeMember(UUID userId, UUID projectId, UUID memberId) {
        MapProject project = permissionService.requireManageMembers(userId, projectId);
        ProjectMember member = memberRepository.findByIdAndProjectId(memberId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Участник не найден"));
        if (member.getRole() == ProjectRole.OWNER) {
            throw new ConflictException("Нельзя удалить владельца. Сначала передайте владение");
        }
        memberRepository.delete(member);
        auditService.log(userId, project, "MEMBER_REMOVED", "ProjectMember", memberId);
    }

    @Transactional
    public ProjectInvitationResponse invite(UUID userId, UUID projectId, ProjectInviteRequest request) {
        MapProject project = permissionService.requireManageMembers(userId, projectId);
        if (request.role() == ProjectRole.OWNER) {
            throw new IllegalArgumentException("Нельзя пригласить с ролью OWNER");
        }
        String email = request.email().trim().toLowerCase();
        userRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
            if (memberRepository.existsByProjectIdAndUserId(projectId, existing.getId())) {
                throw new ConflictException("Пользователь уже является участником проекта");
            }
        });
        invitationRepository.findByProjectIdAndEmailIgnoreCaseAndStatus(projectId, email, InvitationStatus.PENDING)
                .ifPresent(inv -> {
                    inv.setStatus(InvitationStatus.REVOKED);
                    invitationRepository.save(inv);
                });

        User inviter = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        String token = UUID.randomUUID() + UUID.randomUUID().toString().replace("-", "");
        ProjectInvitation invitation = new ProjectInvitation();
        invitation.setProject(project);
        invitation.setEmail(email);
        invitation.setRole(request.role());
        invitation.setTokenHash(hashToken(token));
        invitation.setInvitedBy(inviter);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        invitation = invitationRepository.save(invitation);

        mailService.sendInvitation(email, project.getTitle(), token);
        auditService.log(userId, project, "INVITATION_CREATED", "ProjectInvitation", invitation.getId(),
                auditService.details("email", email).put("role", request.role().name()));
        return ProjectInvitationMapper.toResponse(invitation, token);
    }

    @Transactional(readOnly = true)
    public List<ProjectInvitationResponse> listInvitations(UUID userId, UUID projectId) {
        permissionService.requireManageMembers(userId, projectId);
        return invitationRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(inv -> ProjectInvitationMapper.toResponse(inv, null))
                .toList();
    }

    @Transactional
    public ProjectInvitationResponse revokeInvitation(UUID userId, UUID projectId, UUID invitationId) {
        MapProject project = permissionService.requireManageMembers(userId, projectId);
        ProjectInvitation invitation = invitationRepository.findByIdAndProjectId(invitationId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Приглашение не найдено"));
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new ConflictException("Можно отозвать только активное приглашение");
        }
        invitation.setStatus(InvitationStatus.REVOKED);
        invitationRepository.save(invitation);
        auditService.log(userId, project, "INVITATION_REVOKED", "ProjectInvitation", invitationId);
        return ProjectInvitationMapper.toResponse(invitation, null);
    }

    @Transactional
    public ProjectMemberResponse acceptInvitation(UUID userId, AcceptInvitationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        ProjectInvitation invitation = invitationRepository
                .findByTokenHashAndStatus(hashToken(request.token()), InvitationStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException("Приглашение не найдено или уже использовано"));
        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new ConflictException("Срок приглашения истёк");
        }
        if (!user.getEmail().equalsIgnoreCase(invitation.getEmail())) {
            throw new ForbiddenOperationException("Приглашение предназначено другому пользователю");
        }
        MapProject project = invitation.getProject();
        ProjectMember member = memberRepository.findByProjectIdAndUserId(project.getId(), userId)
                .orElseGet(ProjectMember::new);
        if (member.getId() == null) {
            member.setProject(project);
            member.setUser(user);
            member.setInvitedBy(invitation.getInvitedBy());
        }
        if (member.getRole() != ProjectRole.OWNER) {
            member.setRole(invitation.getRole());
        }
        member = memberRepository.save(member);
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(Instant.now());
        invitationRepository.save(invitation);
        auditService.log(userId, project, "INVITATION_ACCEPTED", "ProjectInvitation", invitation.getId());
        return ProjectMemberMapper.toResponse(member);
    }

    @Transactional
    public void transferOwnership(UUID userId, UUID projectId, TransferOwnershipRequest request) {
        MapProject project = permissionService.requireTransferOwnership(userId, projectId);
        if (request.newOwnerUserId().equals(userId)) {
            throw new IllegalArgumentException("Нельзя передать владение самому себе");
        }
        ProjectMember currentOwner = memberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ForbiddenOperationException("Вы не являетесь участником проекта"));
        if (currentOwner.getRole() != ProjectRole.OWNER) {
            throw new ForbiddenOperationException("Только владелец может передать владение");
        }
        User newOwnerUser = userRepository.findById(request.newOwnerUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Новый владелец не найден"));
        ProjectMember newOwner = memberRepository.findByProjectIdAndUserId(projectId, newOwnerUser.getId())
                .orElseGet(() -> {
                    ProjectMember m = new ProjectMember();
                    m.setProject(project);
                    m.setUser(newOwnerUser);
                    return m;
                });
        newOwner.setRole(ProjectRole.OWNER);
        currentOwner.setRole(ProjectRole.EDITOR);
        memberRepository.save(currentOwner);
        memberRepository.save(newOwner);
        auditService.log(userId, project, "OWNERSHIP_TRANSFERRED", "ProjectMember", newOwner.getId(),
                auditService.details("fromUserId", userId.toString()).put("toUserId", newOwnerUser.getId().toString()));
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
