package com.jsmc.spacemap.project.service;

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

import com.jsmc.spacemap.project.entity.Organization;
import com.jsmc.spacemap.project.entity.OrganizationMember;
import com.jsmc.spacemap.project.entity.User;
import com.jsmc.spacemap.project.repository.OrganizationMemberRepository;
import com.jsmc.spacemap.project.repository.OrganizationRepository;
import com.jsmc.spacemap.project.repository.UserRepository;
import com.jsmc.spacemap.project.service.exception.ForbiddenOperationException;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceAuthTest {

    @Mock OrganizationRepository organizationRepository;
    @Mock OrganizationMemberRepository memberRepository;
    @Mock UserRepository userRepository;

    @InjectMocks OrganizationService organizationService;

    UUID orgId;
    UUID ownerId;
    UUID viewerId;
    Organization organization;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        viewerId = UUID.randomUUID();
        User owner = new User();
        owner.setId(ownerId);
        organization = new Organization();
        organization.setId(orgId);
        organization.setOwner(owner);
    }

    @Test
    void viewerCannotEdit() {
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        OrganizationMember member = new OrganizationMember();
        member.setRole("VIEWER");
        User viewer = new User();
        viewer.setId(viewerId);
        member.setUser(viewer);
        member.setOrganization(organization);
        when(memberRepository.findByOrganizationIdAndUserId(orgId, viewerId)).thenReturn(Optional.of(member));

        assertThrows(ForbiddenOperationException.class,
                () -> organizationService.requireOrganizationEdit(viewerId, orgId));
    }

    @Test
    void ownerCanEdit() {
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        organizationService.requireOrganizationEdit(ownerId, orgId);
    }

    @Test
    void nonMemberCannotEdit() {
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(memberRepository.findByOrganizationIdAndUserId(orgId, viewerId)).thenReturn(Optional.empty());
        assertThrows(ForbiddenOperationException.class,
                () -> organizationService.requireOrganizationEdit(viewerId, orgId));
    }
}
