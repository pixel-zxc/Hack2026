package com.jsmc.spacemap.project.mapper;

import com.jsmc.spacemap.project.dto.response.OrganizationMemberResponse;
import com.jsmc.spacemap.project.entity.OrganizationMember;

public final class OrganizationMemberMapper {
    private OrganizationMemberMapper() { }

    public static OrganizationMemberResponse toResponse(OrganizationMember member) {
        if (member == null) return null;
        return new OrganizationMemberResponse(member.getId(), member.getOrganization().getId(), member.getUser().getId(), member.getUser().getEmail(), member.getUser().getDisplayName(), member.getRole(), member.getCreatedAt());
    }
}
