package com.jsmc.spacemap.project.service;

import com.jsmc.spacemap.project.dto.request.OrganizationCreateRequest;
import com.jsmc.spacemap.project.dto.request.OrganizationUpdateRequest;
import com.jsmc.spacemap.project.dto.response.OrganizationResponse;
import com.jsmc.spacemap.project.entity.Organization;
import com.jsmc.spacemap.project.entity.OrganizationMember;
import com.jsmc.spacemap.project.entity.User;
import com.jsmc.spacemap.project.mapper.OrganizationMapper;
import com.jsmc.spacemap.project.repository.OrganizationMemberRepository;
import com.jsmc.spacemap.project.repository.OrganizationRepository;
import com.jsmc.spacemap.project.repository.UserRepository;
import com.jsmc.spacemap.project.service.exception.ForbiddenOperationException;
import com.jsmc.spacemap.project.service.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrganizationService {
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final UserRepository userRepository;

    public OrganizationService(OrganizationRepository organizationRepository, OrganizationMemberRepository memberRepository, UserRepository userRepository) {
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public OrganizationResponse create(UUID userId, OrganizationCreateRequest request) {
        User owner = findUser(userId);
        Organization organization = new Organization();
        organization.setName(request.name().trim());
        organization.setOwner(owner);
        organization = organizationRepository.save(organization);

        OrganizationMember membership = new OrganizationMember();
        membership.setOrganization(organization);
        membership.setUser(owner);
        membership.setRole("OWNER");
        memberRepository.save(membership);
        return OrganizationMapper.toResponse(organization);
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> getMine(UUID userId) {
        return memberRepository.findByUserId(userId).stream()
                .map(OrganizationMember::getOrganization)
                .map(OrganizationMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getById(UUID userId, UUID organizationId) {
        Organization organization = requireOrganizationAccess(userId, organizationId);
        return OrganizationMapper.toResponse(organization);
    }

    @Transactional
    public OrganizationResponse update(UUID userId, UUID organizationId, OrganizationUpdateRequest request) {
        Organization organization = requireOrganizationOwner(userId, organizationId);
        organization.setName(request.name().trim());
        return OrganizationMapper.toResponse(organizationRepository.save(organization));
    }

    public Organization requireOrganizationAccess(UUID userId, UUID organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Организация не найдена"));
        if (!organization.getOwner().getId().equals(userId)
                && !memberRepository.existsByOrganizationIdAndUserId(organizationId, userId)) {
            throw new ForbiddenOperationException("Нет доступа к организации");
        }
        return organization;
    }

    public Organization requireOrganizationOwner(UUID userId, UUID organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Организация не найдена"));
        if (!organization.getOwner().getId().equals(userId)) {
            throw new ForbiddenOperationException("Только владелец может выполнить эту операцию");
        }
        return organization;
    }

    /**
     * Доступ на изменение: владелец организации либо член с ролью OWNER/EDITOR.
     * VIEWER и отсутствие членства — запрет.
     */
    public Organization requireOrganizationEdit(UUID userId, UUID organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Организация не найдена"));
        if (organization.getOwner().getId().equals(userId)) {
            return organization;
        }
        OrganizationMember member = memberRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .orElseThrow(() -> new ForbiddenOperationException("Нет доступа к организации"));
        String role = member.getRole() == null ? "" : member.getRole().trim().toUpperCase();
        if ("VIEWER".equals(role)) {
            throw new ForbiddenOperationException("Роль VIEWER не позволяет изменять данные");
        }
        if (!"OWNER".equals(role) && !"EDITOR".equals(role)) {
            throw new ForbiddenOperationException("Недостаточно прав для изменения");
        }
        return organization;
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }
}
